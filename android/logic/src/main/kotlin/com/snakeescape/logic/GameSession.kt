package com.snakeescape.logic

import java.util.UUID

/**
 * Lý do kết thúc một vòng chơi (Run).
 */
enum class RunTerminationReason {
    OUT_OF_HEARTS,  // Hết 3 tim trong một màn
    ABANDONED       // Người chơi chủ động thoát/bỏ dở run
}

/**
 * Trạng thái dữ liệu của vòng chơi đang diễn ra (Active Run).
 */
data class ActiveRunState(
    val runId: String = UUID.randomUUID().toString(),
    val currentLevel: Int = 1,
    val levelHearts: Int = ScoringConfig.INITIAL_LEVEL_HEARTS, // 3 tim mỗi khi vào màn mới
    val accumulatedScore: Long = 0L,
    val totalTimeMs: Long = 0L,
    val levelsCleared: Int = 0,
    val isLevelInProgress: Boolean = false,
    val heartsLostInLevel: Int = 0,
    val hintsUsedInLevel: Int = 0,
    val gridVisionsUsedInLevel: Int = 0,
    val currentLevelStartTimeMs: Long = 0L
)

/**
 * Kết quả tổng kết khi Run kết thúc.
 */
data class RunSummary(
    val runId: String,
    val playerName: String,
    val levelsCleared: Int,
    val totalScore: Long,
    val totalTimeMs: Long,
    val rank: Int = 0,
    val isNewPersonalBest: Boolean = false
)

/**
 * Kết quả xử lý khi thất bại trong 1 màn.
 */
sealed class LevelFailureResult {
    /** Còn tim trong màn này: cho phép vào lại đúng màn đó, reset đồng hồ từ đầu */
    data class CanRetry(val heartsRemaining: Int) : LevelFailureResult()

    /** Hết 3 tim trong màn: run kết thúc, rớt thẳng về Level 1 với điểm 0 */
    data class RunOver(val summary: RunSummary) : LevelFailureResult()
}

/**
 * Quản lý vòng đời Run và hệ thống tim độc lập theo từng màn.
 * - Mỗi run luôn bắt đầu từ level 1, điểm 0.
 * - Mỗi màn độc lập: tim reset về 3, đồng hồ reset về giới hạn của màn.
 * - Mất 1 tim khi thất bại (hết giờ / đâm / sai luật).
 * - Hết 3 tim trong màn thì kết thúc run.
 * - Chống lách luật: thoát app giữa màn tính là mất 1 tim của màn đó.
 */
class GameSession(
    initialState: ActiveRunState = ActiveRunState()
) {
    var state: ActiveRunState = initialState
        private set

    /**
     * Bắt đầu một vòng chơi hoàn toàn mới (Level 1, điểm 0, 3 tim).
     */
    fun startNewRun(): ActiveRunState {
        state = ActiveRunState(
            runId = UUID.randomUUID().toString(),
            currentLevel = 1,
            levelHearts = ScoringConfig.INITIAL_LEVEL_HEARTS,
            accumulatedScore = 0L,
            totalTimeMs = 0L,
            levelsCleared = 0,
            isLevelInProgress = true,
            heartsLostInLevel = 0,
            hintsUsedInLevel = 0,
            gridVisionsUsedInLevel = 0,
            currentLevelStartTimeMs = System.currentTimeMillis()
        )
        return state
    }

    /**
     * Vào một màn chơi mới trong run.
     * Reset tim đủ 3, đặt lại thời gian, reset số booster đã dùng trong màn.
     */
    fun enterLevel(level: Int): ActiveRunState {
        state = state.copy(
            currentLevel = level,
            levelHearts = ScoringConfig.INITIAL_LEVEL_HEARTS, // Reset về 3 tim mỗi màn mới
            isLevelInProgress = true,
            heartsLostInLevel = 0,
            hintsUsedInLevel = 0,
            gridVisionsUsedInLevel = 0,
            currentLevelStartTimeMs = System.currentTimeMillis()
        )
        return state
    }

    /**
     * Ghi nhận sử dụng Hint trong màn hiện tại (để trừ điểm khi hoàn thành).
     */
    fun recordHintUsed() {
        state = state.copy(hintsUsedInLevel = state.hintsUsedInLevel + 1)
    }

    /**
     * Ghi nhận sử dụng Grid Vision trong màn hiện tại (để trừ điểm khi hoàn thành).
     */
    fun recordGridVisionUsed() {
        state = state.copy(gridVisionsUsedInLevel = state.gridVisionsUsedInLevel + 1)
    }

    /**
     * Vượt qua màn chơi thành công.
     * Tính điểm bằng ScoreCalculator (thời gian tính là của lần qua màn thành công,
     * không cộng dồn các lần thất bại).
     */
    fun onLevelSuccess(
        solveTimeSeconds: Double,
        timeLimitSeconds: Int,
        playerName: String = ""
    ): LevelScoreBreakdown {
        val breakdown = ScoreCalculator.calculate(
            level = state.currentLevel,
            solveTimeSeconds = solveTimeSeconds,
            timeLimitSeconds = timeLimitSeconds,
            hintsUsed = state.hintsUsedInLevel,
            gridVisionsUsed = state.gridVisionsUsedInLevel,
            heartsLost = state.heartsLostInLevel
        )

        val solveTimeMs = (solveTimeSeconds * 1000).toLong()

        state = state.copy(
            accumulatedScore = state.accumulatedScore + breakdown.finalScore,
            totalTimeMs = state.totalTimeMs + solveTimeMs,
            levelsCleared = state.levelsCleared + 1,
            isLevelInProgress = false // Màn đã hoàn thành an toàn
        )

        return breakdown
    }

    /**
     * Xử lý thất bại trong màn (hết giờ, va chạm hết tim màn chơi, thoát giữa chừng).
     * Mất 1 tim của màn đó.
     * Nếu còn tim: retry màn đó, đồng hồ chạy lại từ đầu.
     * Nếu hết 3 tim: run kết thúc ngay lập tức.
     */
    fun onLevelFailure(playerName: String = ""): LevelFailureResult {
        val newHearts = state.levelHearts - 1
        val newHeartsLost = state.heartsLostInLevel + 1

        return if (newHearts > 0) {
            state = state.copy(
                levelHearts = newHearts,
                heartsLostInLevel = newHeartsLost,
                currentLevelStartTimeMs = System.currentTimeMillis(),
                isLevelInProgress = true
            )
            LevelFailureResult.CanRetry(heartsRemaining = newHearts)
        } else {
            // Hết 3 tim trong màn: Run kết thúc
            val summary = createRunSummary(playerName)
            state = state.copy(
                levelHearts = 0,
                heartsLostInLevel = newHeartsLost,
                isLevelInProgress = false
            )
            LevelFailureResult.RunOver(summary)
        }
    }

    /**
     * Kiểm tra chống lách luật khi mở app:
     * Nếu người chơi thoát app giữa một màn thì tính là mất 1 tim của màn đó.
     */
    fun applyMidGameExitPenalty(playerName: String = ""): LevelFailureResult? {
        if (!state.isLevelInProgress) return null

        // Phạt mất 1 tim do thoát app giữa chừng
        return onLevelFailure(playerName)
    }

    /**
     * Tạo bản tổng kết run.
     */
    fun createRunSummary(playerName: String): RunSummary {
        return RunSummary(
            runId = state.runId,
            playerName = playerName,
            levelsCleared = state.levelsCleared,
            totalScore = state.accumulatedScore,
            totalTimeMs = state.totalTimeMs
        )
    }

    /**
     * Khôi phục trạng thái từ bộ nhớ (DataStore / UserDefaults).
     */
    fun restoreFromState(savedState: ActiveRunState) {
        state = savedState
    }
}
