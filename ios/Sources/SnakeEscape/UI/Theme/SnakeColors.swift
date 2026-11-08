import SwiftUI

public struct SnakeColorTheme: Equatable {
    public let body: Color
    public let bodyDark: Color
    public let eye: Color

    public init(body: Color, bodyDark: Color, eye: Color) {
        self.body = body
        self.bodyDark = bodyDark
        self.eye = eye
    }
}

public struct GameColorScheme: Equatable {
    public let background: Color
    public let boardBackground: Color
    public let gridLine: Color
    public let cellEmpty: Color
    public let cellLocked: Color
    public let cellLockedBorder: Color
    public let surface: Color
    public let surfaceVariant: Color
    public let textPrimary: Color
    public let textSecondary: Color
    public let isDark: Bool
}

public enum SnakeColors {
    // Dark Theme Colors
    public static let darkScheme = GameColorScheme(
        background = Color(red: 0.06, green: 0.09, blue: 0.16),       // Slate 900 #0F172A
        boardBackground = Color(red: 0.12, green: 0.16, blue: 0.23),  // Slate 800 #1E293B
        gridLine = Color(red: 0.20, green: 0.25, blue: 0.33),         // Slate 700 #334155
        cellEmpty = Color(red: 0.09, green: 0.13, blue: 0.20),        // Deep navy #162032
        cellLocked = Color(red: 0.04, green: 0.06, blue: 0.10),       // Cutout black #0B0F19
        cellLockedBorder = Color(red: 0.14, green: 0.18, blue: 0.26),
        surface = Color(red: 0.12, green: 0.16, blue: 0.23),
        surfaceVariant = Color(red: 0.15, green: 0.21, blue: 0.29),
        textPrimary = Color(red: 0.97, green: 0.98, blue: 0.99),
        textSecondary = Color(red: 0.58, green: 0.64, blue: 0.72),
        isDark = true
    )

    // Light Theme Colors
    public static let lightScheme = GameColorScheme(
        background = Color(red: 0.95, green: 0.96, blue: 0.98),       // Slate 100 #F1F5F9
        boardBackground = Color.white,
        gridLine = Color(red: 0.89, green: 0.91, blue: 0.94),         // Slate 200 #E2E8F0
        cellEmpty = Color(red: 0.97, green: 0.98, blue: 0.99),
        cellLocked = Color(red: 0.80, green: 0.84, blue: 0.88),       // Slate 300 #CBD5E1
        cellLockedBorder = Color(red: 0.58, green: 0.64, blue: 0.72),
        surface = Color.white,
        surfaceVariant = Color(red: 0.89, green: 0.91, blue: 0.94),
        textPrimary = Color(red: 0.06, green: 0.09, blue: 0.16),
        textSecondary = Color(red: 0.39, green: 0.45, blue: 0.55),
        isDark = false
    )

    // 8 Bảng màu Pastel / Neon rực rỡ dành cho các con rắn
    public static let snakePalettes: [SnakeColorTheme] = [
        SnakeColorTheme(
            body: Color(red: 0.00, green: 0.90, blue: 0.75), // Neon Mint #00E5C0
            bodyDark: Color(red: 0.00, green: 0.66, blue: 0.55),
            eye: Color(red: 0.00, green: 0.22, blue: 0.18)
        ),
        SnakeColorTheme(
            body: Color(red: 1.00, green: 0.16, blue: 0.52), // Neon Rose #FF2A85
            bodyDark: Color(red: 0.78, green: 0.08, blue: 0.39),
            eye: Color(red: 0.24, green: 0.00, blue: 0.11)
        ),
        SnakeColorTheme(
            body: Color(red: 0.43, green: 0.86, blue: 0.00), // Neon Lime #6EDC00
            bodyDark: Color(red: 0.31, green: 0.63, blue: 0.00),
            eye: Color(red: 0.10, green: 0.22, blue: 0.00)
        ),
        SnakeColorTheme(
            body: Color(red: 1.00, green: 0.62, blue: 0.00), // Electric Amber #FF9E00
            bodyDark: Color(red: 0.80, green: 0.49, blue: 0.00),
            eye: Color(red: 0.28, green: 0.17, blue: 0.00)
        ),
        SnakeColorTheme(
            body: Color(red: 0.61, green: 0.32, blue: 0.88), // Cyber Violet #9B51E0
            bodyDark: Color(red: 0.46, green: 0.18, blue: 0.70),
            eye: Color(red: 0.17, green: 0.04, blue: 0.30)
        ),
        SnakeColorTheme(
            body: Color(red: 0.00, green: 0.71, blue: 0.85), // Ocean Blue #00B4D8
            bodyDark: Color(red: 0.00, green: 0.54, blue: 0.65),
            eye: Color(red: 0.00, green: 0.17, blue: 0.21)
        ),
        SnakeColorTheme(
            body: Color(red: 1.00, green: 0.82, blue: 0.40), // Pastel Gold #FFD166
            bodyDark: Color(red: 0.84, green: 0.65, blue: 0.22),
            eye: Color(red: 0.30, green: 0.22, blue: 0.02)
        ),
        SnakeColorTheme(
            body: Color(red: 1.00, green: 0.40, blue: 0.52), // Soft Coral #FF6584
            bodyDark: Color(red: 0.84, green: 0.23, blue: 0.36),
            eye: Color(red: 0.29, green: 0.04, blue: 0.10)
        )
    ]

    public static let blockedRed = Color(red: 1.00, green: 0.09, blue: 0.27)
    public static let blockedGlow = Color(red: 1.00, green: 0.09, blue: 0.27, opacity: 0.4)
    public static let hintGlow = Color(red: 1.00, green: 0.84, blue: 0.00)
    public static let laserVision = Color(red: 0.00, green: 0.90, blue: 1.00)

    public static func getTheme(_ colorId: Int) -> SnakeColorTheme {
        let safeIndex = (colorId % snakePalettes.count + snakePalettes.count) % snakePalettes.count
        return snakePalettes[safeIndex]
    }
}
