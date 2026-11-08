package com.snakeescape.logic

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Cấu hình tập trung toàn bộ tham số tính điểm, hệ số tốc độ, thời gian giới hạn và phí booster.
 * Gom toàn bộ hằng số và công thức vào 1 file cấu hình riêng để dễ chỉnh.
 */
object ScoringConfig {

    // --- CẤU HÌNH TIM & VÒNG CHƠI ---
    const val INITIAL_LEVEL_HEARTS: Int = 3

    // --- CẤU HÌNH ĐIỂM CƠ BẢN ---
    const val BASE_SCORE_INITIAL: Int = 100
    const val BASE_SCORE_PER_LEVEL: Int = 10
    const val CHALLENGE_BONUS_MULTIPLIER: Double = 1.5

    /**
     * Điểm cơ bản tăng dần theo level: 100 + 10 * level.
     * Chế độ Challenge được nhân thêm hệ số thử thách.
     */
    fun getBaseScore(level: Int, mode: GameMode = GameMode.CAMPAIGN): Int {
        val base = BASE_SCORE_INITIAL + (BASE_SCORE_PER_LEVEL * level)
        return if (mode == GameMode.CHALLENGE) {
            (base * CHALLENGE_BONUS_MULTIPLIER).roundToInt()
        } else {
            base
        }
    }

    // --- CẤU HÌNH HỆ SỐ TỐC ĐỘ (SPEED MULTIPLIER) ---
    const val MIN_SPEED_MULTIPLIER: Double = 0.5
    const val MAX_SPEED_MULTIPLIER: Double = 2.0

    /**
     * Tính hệ số tốc độ dựa trên thời gian giải so với thời gian giới hạn của màn:
     * - Giải càng nhanh (t -> 0) hệ số càng tiến gần 2.0.
     * - Giải sát nút (t -> timeLimit) hệ số tiến về 0.5.
     * - Dải hệ số luôn nằm trong [0.5, 2.0].
     */
    fun getSpeedMultiplier(timeElapsedSeconds: Double, timeLimitSeconds: Int): Double {
        if (timeLimitSeconds <= 0) return MIN_SPEED_MULTIPLIER
        val clampedElapsed = timeElapsedSeconds.coerceIn(0.0, timeLimitSeconds.toDouble())
        val remainingRatio = (timeLimitSeconds - clampedElapsed) / timeLimitSeconds.toDouble()
        val multiplier = MIN_SPEED_MULTIPLIER + ((MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER) * remainingRatio)
        return multiplier.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER)
    }

    // --- PHẠT BOOSTER & MẤT TIM ---
    const val HINT_PENALTY_POINTS: Int = 25
    const val GRID_VISION_PENALTY_POINTS: Int = 25
    const val HEART_LOSS_PENALTY_POINTS: Int = 20

    /**
     * Thời gian giới hạn của từng level (tính bằng giây).
     */
    fun getTimeLimitSeconds(level: Int, mode: GameMode = GameMode.CAMPAIGN): Int {
        return LevelConfig.fromLevel(level, mode).timeLimitSeconds
    }
}

/**
 * Chi tiết điểm số của một màn chơi đã vượt qua thành công.
 */
data class LevelScoreBreakdown(
    val level: Int,
    val baseScore: Int,
    val speedMultiplier: Double,
    val grossScore: Int,
    val hintPenalty: Int,
    val gridVisionPenalty: Int,
    val heartPenalty: Int,
    val finalScore: Int
)

/**
 * Bộ tính điểm độc lập (ScoreCalculator) áp dụng công thức từ ScoringConfig.
 */
object ScoreCalculator {

    /**
     * Tính điểm qua màn thành công:
     * Điểm mỗi màn = điểm_cơ_bản(level) x hệ_số_tốc_độ - phí_booster - phạt_mất_tim.
     * Điểm tối thiểu không âm (>= 0).
     */
    fun calculate(
        level: Int,
        solveTimeSeconds: Double,
        timeLimitSeconds: Int,
        hintsUsed: Int = 0,
        gridVisionsUsed: Int = 0,
        heartsLost: Int = 0,
        mode: GameMode = GameMode.CAMPAIGN
    ): LevelScoreBreakdown {
        val base = ScoringConfig.getBaseScore(level, mode)
        val speedMult = ScoringConfig.getSpeedMultiplier(solveTimeSeconds, timeLimitSeconds)
        val gross = (base * speedMult).roundToInt()

        val hintPen = hintsUsed * ScoringConfig.HINT_PENALTY_POINTS
        val gvPen = gridVisionsUsed * ScoringConfig.GRID_VISION_PENALTY_POINTS
        val heartPen = heartsLost * ScoringConfig.HEART_LOSS_PENALTY_POINTS

        val totalDeduction = hintPen + gvPen + heartPen
        val finalScore = max(0, gross - totalDeduction)

        return LevelScoreBreakdown(
            level = level,
            baseScore = base,
            speedMultiplier = speedMult,
            grossScore = gross,
            hintPenalty = hintPen,
            gridVisionPenalty = gvPen,
            heartPenalty = heartPen,
            finalScore = finalScore
        )
    }
}
