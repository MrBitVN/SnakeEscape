import Foundation

/// Loại chế độ chơi: Campaign (Chiến dịch thường) hoặc Challenge (Thử thách siêu cấp)
public enum GameMode: String, CaseIterable, Codable {
    case campaign = "CAMPAIGN"
    case challenge = "CHALLENGE"
}

/// Loại mặt nạ hình dạng bàn cờ.
public enum ShapeMask: String, CaseIterable, Codable {
    case none = "NONE"
    case circle = "CIRCLE"
    case heart = "HEART"
    case star = "STAR"
    case diamond = "DIAMOND"
    case maze = "MAZE"
    case helix = "HELIX"
    case skull = "SKULL"
}

/// Cấu hình tham số độ khó của một level.
public struct LevelConfig: Equatable {
    public let level: Int
    public let mode: GameMode
    public let width: Int
    public let height: Int
    public let targetSnakes: Int
    public let minLength: Int
    public let maxLength: Int
    public let bendChance: Double
    public let maskType: ShapeMask
    public let hearts: Int
    public let timeLimitSeconds: Int

    public init(
        level: Int,
        mode: GameMode = .campaign,
        width: Int,
        height: Int,
        targetSnakes: Int,
        minLength: Int,
        maxLength: Int,
        bendChance: Double,
        maskType: ShapeMask,
        hearts: Int = 3,
        timeLimitSeconds: Int = 90
    ) {
        self.level = level
        self.mode = mode
        self.width = width
        self.height = height
        self.targetSnakes = targetSnakes
        self.minLength = minLength
        self.maxLength = maxLength
        self.bendChance = bendChance
        self.maskType = maskType
        self.hearts = hearts
        self.timeLimitSeconds = timeLimitSeconds
    }

    public static func fromLevel(_ level: Int, mode: GameMode = .campaign) -> LevelConfig {
        if mode == .campaign {
            return fromCampaignLevel(level)
        } else {
            return fromChallengeLevel(level)
        }
    }

    private static func fromCampaignLevel(_ level: Int) -> LevelConfig {
        let mask: ShapeMask = {
            if level <= 15 { return .none }
            if level <= 35 { return .diamond }
            if level <= 70 { return .circle }
            if level <= 120 { return .heart }
            if level <= 160 { return .star }
            return .maze
        }()

        let (w, h): (Int, Int) = {
            if level <= 5 { return (5, 5) }
            if level <= 15 { return (6, 6) }
            if level <= 35 { return (7, 7) }
            if level <= 70 { return (8, 8) }
            if level <= 160 { return (9, 9) }
            return (10, 10)
        }()

        let snakes: Int = {
            if level <= 5 { return 3 + (level - 1) / 2 }
            if level <= 15 { return 5 + (level - 6) / 3 }
            if level <= 35 { return 7 + (level - 16) / 7 }
            if level <= 70 { return 9 + (level - 36) / 12 }
            if level <= 120 { return 12 + (level - 71) / 16 }
            if level <= 160 { return 14 + (level - 121) / 14 }
            return 16 + (level - 161) / 10
        }()

        let minLen: Int = {
            if level <= 10 { return 2 }
            return 3
        }()

        let maxLen: Int = {
            if level <= 5 { return 3 }
            if level <= 20 { return 4 }
            if level <= 60 { return 5 }
            return 6
        }()

        let bend: Double = {
            if level <= 5 { return 0.35 }
            if level <= 15 { return 0.50 }
            if level <= 35 { return 0.65 }
            if level <= 70 { return 0.75 }
            if level <= 120 { return 0.82 }
            return 0.86
        }()

        let time: Int = {
            if level <= 10 { return 85 }
            if level <= 30 { return 75 }
            if level <= 70 { return 70 + snakes * 2 }
            return 60 + snakes * 3
        }()

        return LevelConfig(
            level: level,
            mode: .campaign,
            width: w,
            height: h,
            targetSnakes: snakes,
            minLength: minLen,
            maxLength: maxLen,
            bendChance: bend,
            maskType: mask,
            hearts: 3,
            timeLimitSeconds: time
        )
    }

    private static func fromChallengeLevel(_ level: Int) -> LevelConfig {
        let mask: ShapeMask = {
            if level <= 10 { return .diamond }
            if level <= 25 { return .helix }
            if level <= 40 { return .skull }
            return .maze
        }()

        let (w, h): (Int, Int) = {
            if level <= 10 { return (10, 10) }
            if level <= 25 { return (11, 11) }
            return (12, 12)
        }()

        let snakes: Int = {
            if level <= 10 { return 12 + (level - 1) / 2 }
            if level <= 25 { return 16 + (level - 11) / 3 }
            if level <= 40 { return 20 + (level - 26) / 3 }
            return 24 + (level - 41) / 2
        }()

        let minLen = 3
        let maxLen: Int = {
            if level <= 10 { return 6 }
            if level <= 25 { return 7 }
            return 8
        }()

        let bend: Double = {
            if level <= 10 { return 0.82 }
            if level <= 25 { return 0.88 }
            if level <= 40 { return 0.92 }
            return 0.95
        }()

        let hearts = (level >= 41) ? 1 : 2
        let time: Int = {
            if level <= 10 { return 80 }
            if level <= 25 { return 75 }
            if level <= 40 { return 70 }
            return 65
        }()

        return LevelConfig(
            level: level,
            mode: .challenge,
            width: w,
            height: h,
            targetSnakes: snakes,
            minLength: minLen,
            maxLength: maxLen,
            bendChance: bend,
            maskType: mask,
            hearts: hearts,
            timeLimitSeconds: time
        )
    }
}
