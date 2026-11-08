import Foundation

public enum RunTerminationReason {
    case outOfHearts
    case abandoned
}

public struct ActiveRunState: Equatable {
    public var runId: String
    public var currentLevel: Int
    public var levelHearts: Int
    public var accumulatedScore: Int64
    public var totalTimeMs: Int64
    public var levelsCleared: Int
    public var isLevelInProgress: Bool
    public var heartsLostInLevel: Int
    public var hintsUsedInLevel: Int
    public var gridVisionsUsedInLevel: Int
    public var currentLevelStartTimeMs: Int64

    public init(
        runId: String = UUID().uuidString,
        currentLevel: Int = 1,
        levelHearts: Int = ScoringConfig.initialLevelHearts,
        accumulatedScore: Int64 = 0,
        totalTimeMs: Int64 = 0,
        levelsCleared: Int = 0,
        isLevelInProgress: Bool = false,
        heartsLostInLevel: Int = 0,
        hintsUsedInLevel: Int = 0,
        gridVisionsUsedInLevel: Int = 0,
        currentLevelStartTimeMs: Int64 = 0
    ) {
        self.runId = runId
        self.currentLevel = currentLevel
        self.levelHearts = levelHearts
        self.accumulatedScore = accumulatedScore
        self.totalTimeMs = totalTimeMs
        self.levelsCleared = levelsCleared
        self.isLevelInProgress = isLevelInProgress
        self.heartsLostInLevel = heartsLostInLevel
        self.hintsUsedInLevel = hintsUsedInLevel
        self.gridVisionsUsedInLevel = gridVisionsUsedInLevel
        self.currentLevelStartTimeMs = currentLevelStartTimeMs
    }
}

public struct RunSummary: Equatable {
    public let runId: String
    public let playerName: String
    public let levelsCleared: Int
    public let totalScore: Int64
    public let totalTimeMs: Int64
    public var rank: Int = 0

    public init(
        runId: String,
        playerName: String,
        levelsCleared: Int,
        totalScore: Int64,
        totalTimeMs: Int64,
        rank: Int = 0
    ) {
        self.runId = runId
        self.playerName = playerName
        self.levelsCleared = levelsCleared
        self.totalScore = totalScore
        self.totalTimeMs = totalTimeMs
        self.rank = rank
    }
}

public enum LevelFailureResult: Equatable {
    case canRetry(heartsRemaining: Int)
    case runOver(summary: RunSummary)
}

public final class GameSession {
    public private(set) var state: ActiveRunState

    public init(initialState: ActiveRunState = ActiveRunState()) {
        self.state = initialState
    }

    @discardableResult
    public func startNewRun() -> ActiveRunState {
        state = ActiveRunState(
            runId: UUID().uuidString,
            currentLevel: 1,
            levelHearts: ScoringConfig.initialLevelHearts,
            accumulatedScore: 0,
            totalTimeMs: 0,
            levelsCleared: 0,
            isLevelInProgress: true,
            heartsLostInLevel: 0,
            hintsUsedInLevel: 0,
            gridVisionsUsedInLevel: 0,
            currentLevelStartTimeMs: Int64(Date().timeIntervalSince1970 * 1000)
        )
        return state
    }

    @discardableResult
    public func enterLevel(_ level: Int) -> ActiveRunState {
        state.currentLevel = level
        state.levelHearts = ScoringConfig.initialLevelHearts
        state.isLevelInProgress = true
        state.heartsLostInLevel = 0
        state.hintsUsedInLevel = 0
        state.gridVisionsUsedInLevel = 0
        state.currentLevelStartTimeMs = Int64(Date().timeIntervalSince1970 * 1000)
        return state
    }

    public func recordHintUsed() {
        state.hintsUsedInLevel += 1
    }

    public func recordGridVisionUsed() {
        state.gridVisionsUsedInLevel += 1
    }

    public func onLevelSuccess(
        solveTimeSeconds: Double,
        timeLimitSeconds: Int,
        playerName: String = ""
    ) -> LevelScoreBreakdown {
        let breakdown = ScoreCalculator.calculate(
            level: state.currentLevel,
            solveTimeSeconds: solveTimeSeconds,
            timeLimitSeconds: timeLimitSeconds,
            hintsUsed: state.hintsUsedInLevel,
            gridVisionsUsed: state.gridVisionsUsedInLevel,
            heartsLost: state.heartsLostInLevel
        )

        let solveTimeMs = Int64(solveTimeSeconds * 1000)
        state.accumulatedScore += Int64(breakdown.finalScore)
        state.totalTimeMs += solveTimeMs
        state.levelsCleared += 1
        state.isLevelInProgress = false

        return breakdown
    }

    public func onLevelFailure(playerName: String = "") -> LevelFailureResult {
        let newHearts = state.levelHearts - 1
        let newHeartsLost = state.heartsLostInLevel + 1

        if newHearts > 0 {
            state.levelHearts = newHearts
            state.heartsLostInLevel = newHeartsLost
            state.currentLevelStartTimeMs = Int64(Date().timeIntervalSince1970 * 1000)
            state.isLevelInProgress = true
            return .canRetry(heartsRemaining: newHearts)
        } else {
            let summary = createRunSummary(playerName: playerName)
            state.levelHearts = 0
            state.heartsLostInLevel = newHeartsLost
            state.isLevelInProgress = false
            return .runOver(summary: summary)
        }
    }

    public func applyMidGameExitPenalty(playerName: String = "") -> LevelFailureResult? {
        guard state.isLevelInProgress else { return nil }
        return onLevelFailure(playerName: playerName)
    }

    public func createRunSummary(playerName: String) -> RunSummary {
        return RunSummary(
            runId: state.runId,
            playerName: playerName,
            levelsCleared: state.levelsCleared,
            totalScore: state.accumulatedScore,
            totalTimeMs: state.totalTimeMs
        )
    }

    public func restoreFromState(_ savedState: ActiveRunState) {
        self.state = savedState
    }
}
