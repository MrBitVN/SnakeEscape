package com.snakeescape.ui.theme

import androidx.compose.runtime.compositionLocalOf

data class AppStrings(
    val appName: String = "Snake Escape",
    val levelPrefix: String = "Level",
    val challengePrefix: String = "Challenge",
    val campaignMode: String = "Campaign",
    val challengeMode: String = "Grandmaster Challenge",
    val timeLabel: String = "Time",
    val scoreLabel: String = "Score",
    val escapedLabel: String = "Escaped",
    val pause: String = "Pause",
    val resume: String = "Resume",
    val settings: String = "Settings",
    val selectLevel: String = "Select Level",
    val nextLevel: String = "Next Level",
    val levelCleared: String = "Level Cleared!",
    val gameOver: String = "Game Over",
    val runOver: String = "Run Over",
    val runSummary: String = "Run Summary",
    val levelsClearedLabel: String = "Levels Cleared",
    val totalScoreLabel: String = "Total Score",
    val totalTimeLabel: String = "Total Time",
    val currentRankLabel: String = "Current Rank",
    val playAgain: String = "Start New Run",
    val quitRun: String = "Quit Run",
    val abandonRun: String = "Abandon Run",
    val outOfHearts: String = "Out of Hearts!",
    val outOfTime: String = "Time's Up!",
    val tryAgainLevel: String = "Try Again",
    val boosterHint: String = "Hint",
    val boosterGridVision: String = "Grid Vision",
    val undo: String = "Undo",
    val soundEffects: String = "Sound Effects",
    val music: String = "Music",
    val haptics: String = "Vibration & Haptics",
    val theme: String = "Theme",
    val themeSystem: String = "System",
    val themeDark: String = "Dark",
    val themeLight: String = "Light",
    val language: String = "Language",
    val langEnglish: String = "English",
    val langVietnamese: String = "Vietnamese",
    val close: String = "Close",
    val locked: String = "Locked",
    val levelDesc: String = "Tap snakes in correct sequence to slither out. Fast solves earn 2x multipliers!",
    val challengeDesc: String = "Grandmaster maze with deeply intertwined snakes and strict heart limit!",
    val coins: String = "Coins",
    val freeCoins: String = "Free Coins",
    val noAds: String = "No Ads",
    val leaderboard: String = "Leaderboard",
    val home: String = "Home",
    val shop: String = "Shop",
    val play: String = "Play",
    val rank: String = "Rank",
    val player: String = "Player",
    val bestTime: String = "Best Time",
    val yourRank: String = "Your Rank",
    val enterNameTitle: String = "Choose Your Player Name",
    val enterNameDesc: String = "Enter a unique nickname (3-16 characters) to compete on real-time leaderboards.",
    val usernameHint: String = "Enter nickname...",
    val usernameTaken: String = "Username is already taken! Please choose another.",
    val usernameAvailable: String = "Username is available!",
    val usernameTooShort: String = "Name must be 3-16 characters (letters, numbers, _).",
    val confirm: String = "Confirm",
    val rewardClaimed: String = "Claimed +50 Coins!",
    val buyBooster: String = "Buy Booster",
    val costCoins: String = "coins",
    val notEnoughCoins: String = "Not enough coins!",
    val midGameExitNotice: String = "Mid-level exit detected: lost 1 heart!"
)

val EnglishStrings = AppStrings()

val LocalAppStrings = compositionLocalOf { EnglishStrings }

object StringsHelper {
    fun getStrings(@Suppress("UNUSED_PARAMETER") languagePref: String): AppStrings {
        return EnglishStrings // 100% English across the entire app
    }
}
