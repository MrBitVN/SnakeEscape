package com.snakeescape.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snakeescape.data.LeaderboardEntry
import com.snakeescape.data.LeaderboardRepository
import com.snakeescape.data.UserPreferencesRepository
import com.snakeescape.data.UserRepository
import com.snakeescape.logic.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    GAMEPLAY
}

enum class GameStatus {
    PLAYING,
    PAUSED,
    WON,
    LEVEL_FAILED_RETRY,   // Mất 1 tim, còn tim -> chuẩn bị retry màn
    RUN_OVER              // Hết 3 tim trong màn -> Run kết thúc
}

data class GameUiState(
    val screenState: ScreenState = ScreenState.HOME,
    val currentLevel: Int = 1,
    val gameMode: GameMode = GameMode.CAMPAIGN,
    val gameState: GameState,
    val initialSnakeCount: Int,
    val escapedSnakeCount: Int = 0,
    val timeRemainingSeconds: Int = 180,
    val levelHearts: Int = ScoringConfig.INITIAL_LEVEL_HEARTS,
    val runScore: Long = 0L,
    val totalTimeMs: Long = 0L,
    val levelsCleared: Int = 0,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val hintSnakeId: Int? = null,
    val isGridVisionActive: Boolean = false,
    val shakingSnakeId: Int? = null,
    val blockedCell: Cell? = null,
    val highestUnlockedLevel: Int = 1,
    val coins: Int = 150,
    val playerName: String = "",
    val playerUid: String = "",
    val isRegistered: Boolean = false,
    val isNameDialogOpen: Boolean = false,
    val isNameDialogMandatory: Boolean = false,
    val nameDialogError: String? = null,
    val isNameRegistering: Boolean = false,
    val isLeaderboardOpen: Boolean = false,
    val isShopOpen: Boolean = false,
    val levelStartTimeMs: Long = 0L,
    val lastScoreBreakdown: LevelScoreBreakdown? = null,
    val earnedCoinsOnWin: Int = 0,
    val hintBoosterCount: Int = 3,
    val gridVisionBoosterCount: Int = 3,
    val isSettingsOpen: Boolean = false,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val language: String = "EN",
    val runSummary: RunSummary? = null,
    val midGamePenaltyNotice: Boolean = false
) {
    val isWon: Boolean get() = gameStatus == GameStatus.WON
    val isRunOver: Boolean get() = gameStatus == GameStatus.RUN_OVER
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)
    val userRepository = UserRepository(application, preferencesRepository)
    val leaderboardRepository = LeaderboardRepository(application, preferencesRepository)

    private val gameSession = GameSession()
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(
        createInitialState(level = 1, mode = GameMode.CAMPAIGN)
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val leaderboardFlow: StateFlow<List<LeaderboardEntry>> = leaderboardRepository.leaderboardFlow

    init {
        // Lắng nghe dữ liệu lưu trữ từ DataStore
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        highestUnlockedLevel = prefs.highestUnlockedLevel,
                        coins = prefs.coins,
                        playerName = prefs.playerName,
                        playerUid = prefs.playerUid,
                        isRegistered = prefs.isRegistered,
                        hintBoosterCount = prefs.hintBoosterCount,
                        gridVisionBoosterCount = prefs.gridVisionBoosterCount,
                        soundEnabled = prefs.soundEnabled,
                        musicEnabled = prefs.musicEnabled,
                        hapticEnabled = prefs.hapticEnabled,
                        themeMode = prefs.themeMode,
                        language = "EN"
                    )
                }

                // Nếu chưa đăng ký tên lần đầu mở app, bắt buộc mở dialog đăng ký tên
                if (!prefs.isRegistered && !_uiState.value.isNameDialogOpen) {
                    _uiState.update {
                        it.copy(isNameDialogOpen = true, isNameDialogMandatory = true)
                    }
                }

                // Bắt đầu lắng nghe BXH realtime
                if (prefs.playerName.isNotBlank()) {
                    leaderboardRepository.startRealtimeListener(prefs.playerName)
                }
            }
        }

        // Chống lách luật: Kiểm tra xem app có bị tắt ngang giữa một màn không
        viewModelScope.launch {
            checkMidGameExitPenalty()
        }
    }

    private fun createInitialState(level: Int, mode: GameMode): GameUiState {
        val config = LevelConfig.fromLevel(level, mode)
        val state = LevelGenerator(level = level, mode = mode)
        return GameUiState(
            screenState = ScreenState.HOME,
            currentLevel = level,
            gameMode = mode,
            gameState = state,
            initialSnakeCount = state.snakes.size,
            escapedSnakeCount = 0,
            timeRemainingSeconds = config.timeLimitSeconds,
            levelHearts = ScoringConfig.INITIAL_LEVEL_HEARTS,
            gameStatus = GameStatus.PLAYING,
            hintSnakeId = null,
            isGridVisionActive = false,
            shakingSnakeId = null,
            blockedCell = null,
            levelStartTimeMs = System.currentTimeMillis()
        )
    }

    /**
     * Bắt đầu một Run mới hoặc tiếp tục màn chơi
     */
    fun startNewRun() {
        val runState = gameSession.startNewRun()
        val config = LevelConfig.fromLevel(1, _uiState.value.gameMode)
        val state = LevelGenerator(level = 1, mode = _uiState.value.gameMode)
            .copy(hearts = ScoringConfig.INITIAL_LEVEL_HEARTS)

        _uiState.update {
            it.copy(
                screenState = ScreenState.GAMEPLAY,
                currentLevel = 1,
                gameState = state,
                initialSnakeCount = state.snakes.size,
                escapedSnakeCount = 0,
                timeRemainingSeconds = config.timeLimitSeconds,
                levelHearts = ScoringConfig.INITIAL_LEVEL_HEARTS,
                runScore = 0L,
                totalTimeMs = 0L,
                levelsCleared = 0,
                gameStatus = GameStatus.PLAYING,
                runSummary = null,
                midGamePenaltyNotice = false,
                levelStartTimeMs = System.currentTimeMillis()
            )
        }

        viewModelScope.launch {
            preferencesRepository.saveActiveRunState(gameSession.state)
        }
        startTimer()
    }

    fun openGame() {
        if (!_uiState.value.isRegistered) {
            _uiState.update { it.copy(isNameDialogOpen = true, isNameDialogMandatory = true) }
            return
        }

        // Mỗi run luôn bắt đầu từ Level 1, điểm 0
        startNewRun()
    }

    fun backToHome() {
        pauseGame()
        _uiState.update { it.copy(screenState = ScreenState.HOME) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value
                if (current.screenState == ScreenState.GAMEPLAY && current.gameStatus == GameStatus.PLAYING) {
                    val newTime = current.timeRemainingSeconds - 1
                    if (newTime <= 0) {
                        _uiState.update { it.copy(timeRemainingSeconds = 0) }
                        onLevelFailed()
                        break
                    } else {
                        _uiState.update { it.copy(timeRemainingSeconds = newTime) }
                    }
                }
            }
        }
    }

    fun onSnakeTapped(
        snakeId: Int,
        onEscaped: (Snake) -> Unit,
        onBlocked: (Snake, Cell) -> Unit
    ) {
        val state = _uiState.value
        if (state.gameStatus != GameStatus.PLAYING) return

        val snake = state.gameState.findSnake(snakeId) ?: return

        if (SnakeEscapeEngine.canEscape(state.gameState, snakeId)) {
            val result = SnakeEscapeEngine.tryMove(state.gameState, snakeId)
            if (result is MoveResult.Escaped) {
                val newEscapedCount = state.escapedSnakeCount + 1
                val isWon = SnakeEscapeEngine.isWon(result.state)

                _uiState.update {
                    it.copy(
                        gameState = result.state,
                        escapedSnakeCount = newEscapedCount,
                        hintSnakeId = null,
                        gameStatus = if (isWon) GameStatus.WON else GameStatus.PLAYING
                    )
                }

                onEscaped(snake)

                if (isWon) {
                    onLevelWon()
                }
            }
        } else {
            // Chết / Đâm va chạm / Bị chặn
            val blockedTarget = snake.head + snake.direction

            _uiState.update {
                it.copy(
                    shakingSnakeId = snakeId,
                    blockedCell = blockedTarget
                )
            }

            onBlocked(snake, blockedTarget)

            // Trừ tim ngay lập tức khi chết / đâm!
            onLevelFailed()
        }
    }

    /**
     * Vượt qua màn chơi thành công.
     */
    private fun onLevelWon() {
        timerJob?.cancel()
        val current = _uiState.value
        val level = current.currentLevel
        val timeLimit = ScoringConfig.getTimeLimitSeconds(level, current.gameMode)
        val timeElapsed = ((System.currentTimeMillis() - current.levelStartTimeMs) / 1000.0).coerceAtLeast(1.0)

        // Tính điểm qua màn theo ScoringConfig
        val breakdown = gameSession.onLevelSuccess(
            solveTimeSeconds = timeElapsed,
            timeLimitSeconds = timeLimit,
            playerName = current.playerName
        )

        // Thưởng coin: cơ bản 30 + thưởng tốc độ
        val earnedCoins = 30 + (current.timeRemainingSeconds / 5).coerceAtLeast(10)

        _uiState.update {
            it.copy(
                runScore = gameSession.state.accumulatedScore,
                totalTimeMs = gameSession.state.totalTimeMs,
                levelsCleared = gameSession.state.levelsCleared,
                lastScoreBreakdown = breakdown,
                earnedCoinsOnWin = earnedCoins,
                gameStatus = GameStatus.WON
            )
        }

        viewModelScope.launch {
            preferencesRepository.addCoins(earnedCoins)
            preferencesRepository.unlockLevel(level + 1)
            preferencesRepository.saveActiveRunState(gameSession.state)
        }
    }

    /**
     * Chuyển sang màn chơi tiếp theo trong Run:
     * - Reset đồng hồ về thời gian giới hạn của màn mới
     * - Reset tim đặt lại đủ 3 (tim không cộng dồn giữa các màn)
     */
    fun nextLevel() {
        val nextLvl = _uiState.value.currentLevel + 1
        val runState = gameSession.enterLevel(nextLvl)
        val config = LevelConfig.fromLevel(nextLvl, _uiState.value.gameMode)
        val newState = LevelGenerator(level = nextLvl, mode = _uiState.value.gameMode)
            .copy(hearts = ScoringConfig.INITIAL_LEVEL_HEARTS)

        _uiState.update {
            it.copy(
                currentLevel = nextLvl,
                gameState = newState,
                initialSnakeCount = newState.snakes.size,
                escapedSnakeCount = 0,
                timeRemainingSeconds = config.timeLimitSeconds,
                levelHearts = ScoringConfig.INITIAL_LEVEL_HEARTS,
                gameStatus = GameStatus.PLAYING,
                hintSnakeId = null,
                isGridVisionActive = false,
                shakingSnakeId = null,
                blockedCell = null,
                levelStartTimeMs = System.currentTimeMillis()
            )
        }

        viewModelScope.launch {
            preferencesRepository.saveActiveRunState(gameSession.state)
        }
        startTimer()
    }

    /**
     * Thất bại trong một màn (chết / đâm va chạm hoặc hết giờ).
     * Mất 1 tim ngay lập tức.
     * Còn tim: cập nhật tim giảm, retry màn đó với đồng hồ chạy lại từ đầu.
     * Hết 3 tim: Run kết thúc ngay lập tức, hiển thị màn tổng kết điểm.
     */
    private fun onLevelFailed() {
        timerJob?.cancel()
        val current = _uiState.value

        when (val failureResult = gameSession.onLevelFailure(playerName = current.playerName)) {
            is LevelFailureResult.CanRetry -> {
                // Trừ tim ngay trên UI
                _uiState.update {
                    it.copy(
                        levelHearts = failureResult.heartsRemaining,
                        gameState = it.gameState.copy(hearts = failureResult.heartsRemaining)
                    )
                }

                val config = LevelConfig.fromLevel(current.currentLevel, current.gameMode)
                val refreshedBoard = LevelGenerator(level = current.currentLevel, mode = current.gameMode)
                    .copy(hearts = failureResult.heartsRemaining)

                viewModelScope.launch {
                    preferencesRepository.saveActiveRunState(gameSession.state)
                    // Cho người chơi thấy hiệu ứng rung đỏ trước khi refresh màn
                    delay(450L)
                    _uiState.update {
                        it.copy(
                            gameState = refreshedBoard,
                            escapedSnakeCount = 0,
                            timeRemainingSeconds = config.timeLimitSeconds,
                            levelHearts = failureResult.heartsRemaining,
                            gameStatus = GameStatus.PLAYING,
                            hintSnakeId = null,
                            isGridVisionActive = false,
                            shakingSnakeId = null,
                            blockedCell = null,
                            levelStartTimeMs = System.currentTimeMillis()
                        )
                    }
                    startTimer()
                }
            }
            is LevelFailureResult.RunOver -> {
                // Hết 3 tim trong màn: Trừ về 0 tim và hiện ngay màn tổng kết điểm
                _uiState.update {
                    it.copy(
                        levelHearts = 0,
                        gameState = it.gameState.copy(hearts = 0)
                    )
                }

                viewModelScope.launch {
                    delay(450L)
                    handleRunOver(failureResult.summary)
                }
            }
        }
    }

    /**
     * Xử lý kết thúc Run (Run Over).
     * Ghi bản ghi vào runs, cập nhật bestScore user, hiện màn tổng kết.
     */
    private fun handleRunOver(summary: RunSummary) {
        _uiState.update {
            it.copy(
                gameStatus = GameStatus.RUN_OVER,
                runSummary = summary
            )
        }

        viewModelScope.launch {
            preferencesRepository.clearActiveRunState()
            leaderboardRepository.recordRunEnd(
                summary = summary,
                uid = _uiState.value.playerUid,
                username = _uiState.value.playerName
            )
        }
    }

    /**
     * Chống lách luật: Nếu thoát app giữa một màn thì tính là mất 1 tim của màn đó.
     */
    private suspend fun checkMidGameExitPenalty() {
        val prefsFlow = preferencesRepository.userPreferencesFlow.first()
        val savedRun = prefsFlow.activeRunState

        if (savedRun.isLevelInProgress) {
            gameSession.restoreFromState(savedRun)
            val penalty = gameSession.applyMidGameExitPenalty(playerName = prefsFlow.playerName)

            if (penalty != null) {
                when (penalty) {
                    is LevelFailureResult.CanRetry -> {
                        _uiState.update {
                            it.copy(
                                levelHearts = penalty.heartsRemaining,
                                midGamePenaltyNotice = true
                            )
                        }
                        preferencesRepository.saveActiveRunState(gameSession.state)
                    }
                    is LevelFailureResult.RunOver -> {
                        handleRunOver(penalty.summary)
                    }
                }
            }
        }
    }

    fun dismissMidGamePenaltyNotice() {
        _uiState.update { it.copy(midGamePenaltyNotice = false) }
    }

    fun abandonCurrentRun() {
        timerJob?.cancel()
        val summary = gameSession.createRunSummary(_uiState.value.playerName)
        handleRunOver(summary)
    }

    fun clearShakeAndBlocked() {
        _uiState.update { it.copy(shakingSnakeId = null, blockedCell = null) }
    }

    fun undo() {
        val state = _uiState.value
        if (state.gameState.history.isNotEmpty()) {
            val prevState = SnakeEscapeEngine.undo(state.gameState)
            val newEscaped = (state.initialSnakeCount - prevState.snakes.size).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    gameState = prevState,
                    escapedSnakeCount = newEscaped,
                    hintSnakeId = null,
                    blockedCell = null
                )
            }
        }
    }

    fun pauseGame() {
        if (_uiState.value.gameStatus == GameStatus.PLAYING) {
            _uiState.update { it.copy(gameStatus = GameStatus.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameStatus == GameStatus.PAUSED) {
            _uiState.update { it.copy(gameStatus = GameStatus.PLAYING) }
        }
    }

    fun useHint() {
        val state = _uiState.value
        if (state.hintBoosterCount <= 0 || state.gameStatus != GameStatus.PLAYING) return

        val suggestedId = SnakeEscapeEngine.hint(state.gameState) ?: return
        viewModelScope.launch {
            if (preferencesRepository.consumeHint()) {
                gameSession.recordHintUsed()
                _uiState.update { it.copy(hintSnakeId = suggestedId) }
            }
        }
    }

    fun useGridVision() {
        val state = _uiState.value
        if (state.gridVisionBoosterCount <= 0 || state.gameStatus != GameStatus.PLAYING) return

        viewModelScope.launch {
            if (preferencesRepository.consumeGridVision()) {
                gameSession.recordGridVisionUsed()
                _uiState.update { it.copy(isGridVisionActive = true) }
                delay(12000L)
                _uiState.update { it.copy(isGridVisionActive = false) }
            }
        }
    }

    fun claimFreeCoins() {
        viewModelScope.launch { preferencesRepository.addCoins(50) }
    }

    fun buyBooster(type: String) {
        val cost = 50
        viewModelScope.launch {
            if (preferencesRepository.consumeCoins(cost)) {
                when (type) {
                    "HINT" -> preferencesRepository.addHint(3)
                    "GRID_VISION" -> preferencesRepository.addGridVision(3)
                }
            }
        }
    }

    fun openNameDialog(isMandatory: Boolean = false) {
        _uiState.update {
            it.copy(
                isNameDialogOpen = true,
                isNameDialogMandatory = isMandatory,
                nameDialogError = null
            )
        }
    }

    fun closeNameDialog() {
        if (!_uiState.value.isNameDialogMandatory) {
            _uiState.update { it.copy(isNameDialogOpen = false, nameDialogError = null) }
        }
    }

    /**
     * Xác nhận đăng ký Username bằng Firebase Atomic Transaction
     */
    fun confirmPlayerName(name: String) {
        _uiState.update { it.copy(isNameRegistering = true, nameDialogError = null) }
        viewModelScope.launch {
            val result = userRepository.registerUsername(name)
            result.onSuccess { registeredName ->
                _uiState.update {
                    it.copy(
                        playerName = registeredName,
                        isRegistered = true,
                        isNameDialogOpen = false,
                        isNameRegistering = false,
                        nameDialogError = null
                    )
                }
                leaderboardRepository.startRealtimeListener(registeredName)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isNameRegistering = false,
                        nameDialogError = error.message ?: "Failed to register username"
                    )
                }
            }
        }
    }

    fun openLeaderboard() {
        if (!_uiState.value.isRegistered) {
            openNameDialog(isMandatory = true)
        } else {
            leaderboardRepository.startRealtimeListener(_uiState.value.playerName)
            _uiState.update { it.copy(isLeaderboardOpen = true) }
        }
    }

    fun closeLeaderboard() {
        _uiState.update { it.copy(isLeaderboardOpen = false) }
    }

    fun openShop() {
        _uiState.update { it.copy(isShopOpen = true) }
    }

    fun closeShop() {
        _uiState.update { it.copy(isShopOpen = false) }
    }

    fun openSettings() {
        pauseGame()
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
        resumeGame()
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMusicEnabled(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticEnabled(enabled) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }
}
