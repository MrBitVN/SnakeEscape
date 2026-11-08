package com.snakeescape.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SnakeColorTheme(
    val body: Color,
    val bodyDark: Color,
    val eye: Color,
    val patternColor: Color = Color.White.copy(alpha = 0.35f)
)

@Immutable
data class GameColorScheme(
    val background: Color,
    val boardBackground: Color,
    val gridLine: Color,
    val cellEmpty: Color,
    val cellLocked: Color,
    val cellLockedBorder: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

object SnakeColors {
    // Purple / Lavender Theme (matching screenshot 1 & 2)
    val LavenderScheme = GameColorScheme(
        background = Color(0xFFDDD6FE),       // Pastel Lavender
        boardBackground = Color(0xFFEDE9FE),  // Soft Lilac
        gridLine = Color(0x33C4B5FD),
        cellEmpty = Color(0x1AFFFFFF),
        cellLocked = Color(0x448B5CF6),
        cellLockedBorder = Color(0x667C3AED),
        surface = Color(0xFF581C87),          // Royal Purple
        surfaceVariant = Color(0xFF6B21A8),   // Vibrant Purple
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFDDD6FE),
        isDark = false
    )

    // Dark Theme Colors
    val DarkScheme = GameColorScheme(
        background = Color(0xFF0F172A),       // Slate 900
        boardBackground = Color(0xFF1E293B),  // Slate 800
        gridLine = Color(0xFF334155),         // Slate 700
        cellEmpty = Color(0xFF162032),        // Soft deep navy
        cellLocked = Color(0xFF0B0F19),       // Cutout black
        cellLockedBorder = Color(0xFF232D42),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF27354A),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        isDark = true
    )

    // Light Theme Colors
    val LightScheme = LavenderScheme

    // Vibrant Snake Palettes (matching Image 2: Red, Black, Green, Baby Blue, Blue, Violet, Orange, Yellow)
    val SnakePalettes = listOf(
        SnakeColorTheme(
            body = Color(0xFFDC2626),      // Vivid Crimson Red
            bodyDark = Color(0xFF991B1B),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFFB91C1C)
        ),
        SnakeColorTheme(
            body = Color(0xFF27272A),      // Charcoal Obsidian Black
            bodyDark = Color(0xFF18181B),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFF3F3F46)
        ),
        SnakeColorTheme(
            body = Color(0xFF22C55E),      // Bright Cartoon Green
            bodyDark = Color(0xFF15803D),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFF16A34A)
        ),
        SnakeColorTheme(
            body = Color(0xFF38BDF8),      // Baby Sky Blue
            bodyDark = Color(0xFF0284C7),
            eye = Color(0xFF0369A1),
            patternColor = Color(0xFF7DD3FC)
        ),
        SnakeColorTheme(
            body = Color(0xFF2563EB),      // Royal Cobalt Blue
            bodyDark = Color(0xFF1D4ED8),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFF60A5FA)
        ),
        SnakeColorTheme(
            body = Color(0xFFA855F7),      // Neon Purple
            bodyDark = Color(0xFF7E22CE),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFFC084FC)
        ),
        SnakeColorTheme(
            body = Color(0xFFF97316),      // Electric Amber Orange
            bodyDark = Color(0xFFC2410C),
            eye = Color(0xFF38BDF8),
            patternColor = Color(0xFFFB923C)
        ),
        SnakeColorTheme(
            body = Color(0xFFFACC15),      // Vibrant Banana Yellow
            bodyDark = Color(0xFFA16207),
            eye = Color(0xFF0284C7),
            patternColor = Color(0xFFFEF08A)
        )
    )

    // Coin & UI Colors
    val GoldCoin = Color(0xFFFFB703)
    val GoldCoinDark = Color(0xFFFB8500)
    val ButtonGreen = Color(0xFF22C55E)
    val ButtonGreenDark = Color(0xFF15803D)
    val HomePurple = Color(0xFF6B21A8)
    val HomePurpleDark = Color(0xFF4C1D95)
    val BlueCard = Color(0xFF0284C7)
    val OrangeTimer = Color(0xFFEA580C)

    val BlockedRed = Color(0xFFFF1744)
    val BlockedGlow = Color(0x55FF1744)
    val HintGlow = Color(0xFF00E676)

    val ChallengeFlame = Color(0xFFFF2A85)
    val ChallengeGold = Color(0xFFFFD700)
    val ChallengePurple = Color(0xFF9C27B0)

    fun getTheme(colorId: Int): SnakeColorTheme {
        val safeIndex = (colorId % SnakePalettes.size + SnakePalettes.size) % SnakePalettes.size
        return SnakePalettes[safeIndex]
    }
}
