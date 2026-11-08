import Foundation
import Combine

public enum GameStatus: Equatable {
    case playing
    case paused
    case won
    case levelFailedRetry
    case runOver
}

public struct GameUiState: Equatable {
    public var currentLevel: Int = 1
    public var gameState: GameState
    public var initialSnakeCount: Int
    public var escapedSnakeCount: Int = 0
    public var timeRemainingSeconds: Int = 180
    public var levelHearts: Int = ScoringConfig.initialLevelHearts
    public var runScore: Int64 = 0
    public var totalTimeMs: Int64 = 0
    public var levelsCleared: Int = 0
    public var gameStatus: GameStatus = .playing
    public var hintSnakeId: Int? = nil
    public var isGridVisionActive: Bool = false
    public var shakingSnakeId: Int? = nil
    public var blockedCell: Cell? = nil
    public var highestUnlockedLevel: Int = 1
    public var coins: Int = 150
    public var playerName: String = ""
    public var isRegistered: Bool = false
    public var isNameDialogOpen: Bool = false
    public var isLeaderboardOpen: Bool = false
    public var hintBoosterCount: Int = 3
    public var gridVisionBoosterCount: Int = 3
    public var isSettingsOpen: Bool = false
    public var soundEnabled: Bool = true
    public var musicEnabled: Bool = true
    public var hapticEnabled: Bool = true
    public var themeMode: String = "SYSTEM"
    public var runSummary: RunSummary? = nil
    public var lastScoreBreakdown: LevelScoreBreakdown? = nil
    public var levelStartTimeMs: Int64 = 0

    public var isGameOver: Bool { gameStatus == .runOver }
    public var isWon: Bool { gameStatus == .won }
}

public final class GameViewModel: ObservableObject {

    @Published public private(set) var uiState: GameUiState

    public let repository: UserPreferencesRepository
    public let userRepository: UserRepository
    public let leaderboardRepository: LeaderboardRepository
    public let gameSession: GameSession

    private var timerCancellable: AnyCancellable?
    private var gridVisionTask: DispatchWorkItem?

    public init(
        repository: UserPreferencesRepository = .shared,
        userRepository: UserRepository = .shared,
        leaderboardRepository: LeaderboardRepository = .shared
    ) {
        self.repository = repository
        self.userRepository = userRepository
        self.leaderboardRepository = leaderboardRepository
        self.gameSession = GameSession()

        let initialLevel = 1
        let config = LevelConfig.fromLevel(initialLevel)
        let initialState = LevelGenerator.generate(level: initialLevel)

        self.uiState = GameUiState(
            currentLevel: initialLevel,
            gameState: initialState,
            initialSnakeCount: initialState.snakes.count,
            escapedSnakeCount: 0,
            timeRemainingSeconds: config.timeLimitSeconds,
            levelHearts: ScoringConfig.initialLevelHearts,
            highestUnlockedLevel: repository.highestUnlockedLevel,
            coins: repository.coins,
            playerName: repository.playerName,
            isRegistered: repository.isRegistered,
            hintBoosterCount: repository.hintBoosterCount,
            gridVisionBoosterCount: repository.gridVisionBoosterCount,
            soundEnabled: repository.soundEnabled,
            musicEnabled: repository.musicEnabled,
            hapticEnabled: repository.hapticEnabled,
            themeMode: repository.themeMode,
            levelStartTimeMs: Int64(Date().timeIntervalSince1970 * 1000)
        )

        // Mid-game anti-cheat penalty check
        checkMidGameExitPenalty()

        // Start real-time listener if registered
        if !repository.playerName.isEmpty {
            leaderboardRepository.startRealtimeListener(currentUsername: repository.playerName)
        }
    }

    public func startNewRun() {
        let runState = gameSession.startNewRun()
        let config = LevelConfig.fromLevel(1)
        var newState = LevelGenerator.generate(level: 1)
        newState.hearts = ScoringConfig.initialLevelHearts

        uiState.currentLevel = 1
        uiState.gameState = newState
        uiState.initialSnakeCount = newState.snakes.count
        uiState.escapedSnakeCount = 0
        uiState.timeRemainingSeconds = config.timeLimitSeconds
        uiState.levelHearts = ScoringConfig.initialLevelHearts
        uiState.runScore = 0
        uiState.totalTimeMs = 0
        uiState.levelsCleared = 0
        uiState.gameStatus = .playing
        uiState.runSummary = nil
        uiState.levelStartTimeMs = Int64(Date().timeIntervalSince1970 * 1000)

        repository.saveActiveRunState(gameSession.state)
        startTimer()
    }

    private func checkMidGameExitPenalty() {
        let saved = repository.getActiveRunState()
        if saved.isLevelInProgress {
            gameSession.restoreFromState(saved)
            if let penalty = gameSession.applyMidGameExitPenalty(playerName: repository.playerName) {
                switch penalty {
                case .canRetry(let hearts):
                    uiState.levelHearts = hearts
                    repository.saveActiveRunState(gameSession.state)
                case .runOver(let summary):
                    handleRunOver(summary)
                }
            }
        }
    }

    private func startTimer() {
        timerCancellable?.cancel()
        timerCancellable = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                guard self.uiState.gameStatus == .playing else { return }

                if self.uiState.timeRemainingSeconds <= 1 {
                    self.uiState.timeRemainingSeconds = 0
                    self.onLevelFailed()
                } else {
                    self.uiState.timeRemainingSeconds -= 1
                }
            }
    }

    public func onSnakeTapped(
        snakeId: Int,
        onEscaped: @escaping (Snake) -> Void,
        onBlocked: @escaping (Snake, Cell) -> Void
    ) {
        guard uiState.gameStatus == .playing else { return }
        guard let snake = uiState.gameState.findSnake(snakeId) else { return }

        if SnakeEscapeEngine.canEscape(state: uiState.gameState, snakeId: snakeId) {
            let result = SnakeEscapeEngine.tryMove(state: uiState.gameState, snakeId: snakeId)
            if case .escaped(let newState, _, _) = result {
                let newEscapedCount = uiState.escapedSnakeCount + 1
                let isWon = SnakeEscapeEngine.isWon(state: newState)

                uiState.gameState = newState
                uiState.escapedSnakeCount = newEscapedCount
                uiState.hintSnakeId = nil
                uiState.gameStatus = isWon ? .won : .playing

                onEscaped(snake)

                if isWon {
                    onLevelWon()
                }
            }
        } else {
            // Chết / Đâm va chạm / Bị chặn
            let blockedTarget = snake.head + snake.direction

            uiState.shakingSnakeId = snakeId
            uiState.blockedCell = blockedTarget

            onBlocked(snake, blockedTarget)

            // Trừ tim ngay lập tức khi chết / đâm!
            onLevelFailed()
        }
    }

    private func onLevelWon() {
        timerCancellable?.cancel()
        let level = uiState.currentLevel
        let limit = ScoringConfig.getTimeLimitSeconds(level: level)
        let elapsed = max(1.0, Double(Int64(Date().timeIntervalSince1970 * 1000) - uiState.levelStartTimeMs) / 1000.0)

        let breakdown = gameSession.onLevelSuccess(
            solveTimeSeconds: elapsed,
            timeLimitSeconds: limit,
            playerName: uiState.playerName
        )

        uiState.runScore = gameSession.state.accumulatedScore
        uiState.totalTimeMs = gameSession.state.totalTimeMs
        uiState.levelsCleared = gameSession.state.levelsCleared
        uiState.lastScoreBreakdown = breakdown
        uiState.gameStatus = .won

        repository.saveActiveRunState(gameSession.state)
    }

    public func nextLevel() {
        let nextLvl = uiState.currentLevel + 1
        gameSession.enterLevel(nextLvl)
        let config = LevelConfig.fromLevel(nextLvl)
        var newState = LevelGenerator.generate(level: nextLvl)
        newState.hearts = ScoringConfig.initialLevelHearts

        uiState.currentLevel = nextLvl
        uiState.gameState = newState
        uiState.initialSnakeCount = newState.snakes.count
        uiState.escapedSnakeCount = 0
        uiState.timeRemainingSeconds = config.timeLimitSeconds
        uiState.levelHearts = ScoringConfig.initialLevelHearts
        uiState.gameStatus = .playing
        uiState.hintSnakeId = nil
        uiState.isGridVisionActive = false
        uiState.shakingSnakeId = nil
        uiState.blockedCell = nil
        uiState.levelStartTimeMs = Int64(Date().timeIntervalSince1970 * 1000)

        repository.saveActiveRunState(gameSession.state)
        startTimer()
    }

    private func onLevelFailed() {
        timerCancellable?.cancel()
        let currentLevel = uiState.currentLevel
        switch gameSession.onLevelFailure(playerName: uiState.playerName) {
        case .canRetry(let heartsRemaining):
            // Trừ tim ngay trên UI
            uiState.levelHearts = heartsRemaining
            uiState.gameState.hearts = heartsRemaining

            let config = LevelConfig.fromLevel(currentLevel)
            var refreshed = LevelGenerator.generate(level: currentLevel)
            refreshed.hearts = heartsRemaining

            repository.saveActiveRunState(gameSession.state)

            // Cho người chơi thấy hiệu ứng rung đỏ trước khi refresh màn
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self] in
                guard let self = self else { return }
                self.uiState.gameState = refreshed
                self.uiState.escapedSnakeCount = 0
                self.uiState.timeRemainingSeconds = config.timeLimitSeconds
                self.uiState.levelHearts = heartsRemaining
                self.uiState.gameStatus = .playing
                self.uiState.hintSnakeId = nil
                self.uiState.isGridVisionActive = false
                self.uiState.shakingSnakeId = nil
                self.uiState.blockedCell = nil
                self.uiState.levelStartTimeMs = Int64(Date().timeIntervalSince1970 * 1000)
                self.startTimer()
            }

        case .runOver(let summary):
            // Hết 3 tim trong màn: Trừ về 0 tim và hiện ngay màn tổng kết điểm
            uiState.levelHearts = 0
            uiState.gameState.hearts = 0

            DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self] in
                self?.handleRunOver(summary)
            }
        }
    }

    private func handleRunOver(_ summary: RunSummary) {
        uiState.gameStatus = .runOver
        uiState.runSummary = summary

        repository.clearActiveRunState()
        leaderboardRepository.recordRunEnd(
            summary: summary,
            uid: repository.playerUid,
            username: repository.playerName
        )
    }

    public func abandonCurrentRun() {
        timerCancellable?.cancel()
        let summary = gameSession.createRunSummary(playerName: uiState.playerName)
        handleRunOver(summary)
    }

    public func clearShakeAndBlocked() {
        uiState.shakingSnakeId = nil
        uiState.blockedCell = nil
    }

    public func undo() {
        guard !uiState.gameState.history.isEmpty else { return }
        let prevState = SnakeEscapeEngine.undo(state: uiState.gameState)
        let newEscaped = max(0, uiState.initialSnakeCount - prevState.snakes.count)
        uiState.gameState = prevState
        uiState.escapedSnakeCount = newEscaped
        uiState.hintSnakeId = nil
        uiState.blockedCell = nil
    }

    public func pauseGame() {
        if uiState.gameStatus == .playing {
            uiState.gameStatus = .paused
        }
    }

    public func resumeGame() {
        if uiState.gameStatus == .paused {
            uiState.gameStatus = .playing
        }
    }

    public func useHint() {
        guard uiState.hintBoosterCount > 0, uiState.gameStatus == .playing else { return }
        guard let suggestedId = SnakeEscapeEngine.hint(state: uiState.gameState) else { return }

        if repository.consumeHint() {
            gameSession.recordHintUsed()
            uiState.hintBoosterCount = repository.hintBoosterCount
            uiState.hintSnakeId = suggestedId
        }
    }

    public func useGridVision() {
        guard uiState.gridVisionBoosterCount > 0, uiState.gameStatus == .playing else { return }

        if repository.consumeGridVision() {
            gameSession.recordGridVisionUsed()
            uiState.gridVisionBoosterCount = repository.gridVisionBoosterCount
            uiState.isGridVisionActive = true

            gridVisionTask?.cancel()
            let task = DispatchWorkItem { [weak self] in
                self?.uiState.isGridVisionActive = false
            }
            gridVisionTask = task
            DispatchQueue.main.asyncAfter(deadline: .now() + 12.0, execute: task)
        }
    }

    public func openSettings() {
        pauseGame()
        uiState.isSettingsOpen = true
    }

    public func closeSettings() {
        uiState.isSettingsOpen = false
        resumeGame()
    }

    public func setSoundEnabled(_ enabled: Bool) {
        repository.soundEnabled = enabled
        uiState.soundEnabled = enabled
    }

    public func setMusicEnabled(_ enabled: Bool) {
        repository.musicEnabled = enabled
        uiState.musicEnabled = enabled
    }

    public func setHapticEnabled(_ enabled: Bool) {
        repository.hapticEnabled = enabled
        uiState.hapticEnabled = enabled
    }

    public func setThemeMode(_ mode: String) {
        repository.themeMode = mode
        uiState.themeMode = mode
    }
}
