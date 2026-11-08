import Foundation

/// Cấu hình tập trung toàn bộ tham số tính điểm, hệ số tốc độ, thời gian giới hạn và phí booster.
/// Giữ nguyên 1:1 công thức và hằng số như Android.
public enum ScoringConfig {

    // --- CẤU HÌNH TIM & VÒNG CHƠI ---
    public static let initialLevelHearts: Int = 3

    // --- CẤU HÌNH ĐIỂM CƠ BẢN ---
    public static let baseScoreInitial: Int = 100
    public static let baseScorePerLevel: Int = 10
    public static let challengeBonusMultiplier: Double = 1.5

    /// Điểm cơ bản tăng dần theo level: 100 + 10 * level.
    public static func getBaseScore(level: Int, isChallenge: Bool = false) -> Int {
        let base = baseScoreInitial + (baseScorePerLevel * level)
        if isChallenge {
            return Int((Double(base) * challengeBonusMultiplier).rounded())
        } else {
            return base
        }
    }

    // --- CẤU HÌNH HỆ SỐ TỐC ĐỘ (SPEED MULTIPLIER) ---
    public static let minSpeedMultiplier: Double = 0.5
    public static let maxSpeedMultiplier: Double = 2.0

    /// Tính hệ số tốc độ dựa trên thời gian giải so với thời gian giới hạn của màn:
    /// - Giải càng nhanh (t -> 0) hệ số càng tiến gần 2.0.
    /// - Giải sát nút (t -> timeLimit) hệ số tiến về 0.5.
    public static func getSpeedMultiplier(timeElapsedSeconds: Double, timeLimitSeconds: Int) -> Double {
        guard timeLimitSeconds > 0 else { return minSpeedMultiplier }
        let clampedElapsed = min(max(timeElapsedSeconds, 0.0), Double(timeLimitSeconds))
        let remainingRatio = (Double(timeLimitSeconds) - clampedElapsed) / Double(timeLimitSeconds)
        let multiplier = minSpeedMultiplier + ((maxSpeedMultiplier - minSpeedMultiplier) * remainingRatio)
        return min(max(multiplier, minSpeedMultiplier), maxSpeedMultiplier)
    }

    // --- PHẠT BOOSTER & MẤT TIM ---
    public static let hintPenaltyPoints: Int = 25
    public static let gridVisionPenaltyPoints: Int = 25
    public static let heartLossPenaltyPoints: Int = 20

    /// Thời gian giới hạn của từng level
    public static func getTimeLimitSeconds(level: Int) -> Int {
        return LevelConfig.fromLevel(level).timeLimitSeconds
    }
}

/// Chi tiết điểm số của màn chơi
public struct LevelScoreBreakdown: Equatable {
    public let level: Int
    public let baseScore: Int
    public let speedMultiplier: Double
    public let grossScore: Int
    public let hintPenalty: Int
    public let gridVisionPenalty: Int
    public let heartPenalty: Int
    public let finalScore: Int

    public init(
        level: Int,
        baseScore: Int,
        speedMultiplier: Double,
        grossScore: Int,
        hintPenalty: Int,
        gridVisionPenalty: Int,
        heartPenalty: Int,
        finalScore: Int
    ) {
        self.level = level
        self.baseScore = baseScore
        self.speedMultiplier = speedMultiplier
        self.grossScore = grossScore
        self.hintPenalty = hintPenalty
        self.gridVisionPenalty = gridVisionPenalty
        self.heartPenalty = heartPenalty
        self.finalScore = finalScore
    }
}

/// Bộ tính điểm độc lập (ScoreCalculator)
public enum ScoreCalculator {

    public static func calculate(
        level: Int,
        solveTimeSeconds: Double,
        timeLimitSeconds: Int,
        hintsUsed: Int = 0,
        gridVisionsUsed: Int = 0,
        heartsLost: Int = 0,
        isChallenge: Bool = false
    ) -> LevelScoreBreakdown {
        let base = ScoringConfig.getBaseScore(level: level, isChallenge: isChallenge)
        let speedMult = ScoringConfig.getSpeedMultiplier(timeElapsedSeconds: solveTimeSeconds, timeLimitSeconds: timeLimitSeconds)
        let gross = Int((Double(base) * speedMult).rounded())

        let hintPen = hintsUsed * ScoringConfig.hintPenaltyPoints
        let gvPen = gridVisionsUsed * ScoringConfig.gridVisionPenaltyPoints
        let heartPen = heartsLost * ScoringConfig.heartLossPenaltyPoints

        let totalDeduction = hintPen + gvPen + heartPen
        let finalScore = max(0, gross - totalDeduction)

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
