import Foundation

public struct AppStrings: Equatable {
    public let appName: String
    public let levelPrefix: String
    public let timeLabel: String
    public let scoreLabel: String
    public let escapedLabel: String
    public let pause: String
    public let resume: String
    public let settings: String
    public let selectLevel: String
    public let nextLevel: String
    public let levelCleared: String
    public let gameOver: String
    public let runOver: String
    public let runSummary: String
    public let levelsClearedLabel: String
    public let totalScoreLabel: String
    public let totalTimeLabel: String
    public let currentRankLabel: String
    public let playAgain: String
    public let abandonRun: String
    public let outOfHearts: String
    public let outOfTime: String
    public let boosterHint: String
    public let boosterGridVision: String
    public let undo: String
    public let soundEffects: String
    public let music: String
    public let haptics: String
    public let theme: String
    public let themeSystem: String
    public let themeDark: String
    public let themeLight: String
    public let language: String
    public let langEnglish: String
    public let close: String
    public let locked: String
    public let levelDesc: String
    public let leaderboard: String
    public let player: String
    public let rank: String
    public let home: String
}

public enum StringsHelper {
    public static let english = AppStrings(
        appName: "Snake Escape",
        levelPrefix: "Level",
        timeLabel: "Time",
        scoreLabel: "Score",
        escapedLabel: "Escaped",
        pause: "Pause",
        resume: "Resume",
        settings: "Settings",
        selectLevel: "Select Level",
        nextLevel: "Next Level",
        levelCleared: "Level Cleared!",
        gameOver: "Game Over",
        runOver: "Run Over",
        runSummary: "Run Summary",
        levelsClearedLabel: "Levels Cleared",
        totalScoreLabel: "Total Score",
        totalTimeLabel: "Total Time",
        currentRankLabel: "Current Rank",
        playAgain: "Start New Run",
        abandonRun: "Abandon Run",
        outOfHearts: "Out of Hearts!",
        outOfTime: "Time's Up!",
        boosterHint: "Hint",
        boosterGridVision: "Grid Vision",
        undo: "Undo",
        soundEffects: "Sound Effects",
        music: "Background Music",
        haptics: "Haptic Feedback",
        theme: "Theme",
        themeSystem: "System",
        themeDark: "Dark",
        themeLight: "Light",
        language: "Language",
        langEnglish: "English",
        close: "Close",
        locked: "Locked",
        levelDesc: "Tap snakes in correct sequence to slither out. Fast solves earn 2x multipliers!",
        leaderboard: "Leaderboard",
        player: "Player",
        rank: "Rank",
        home: "Home"
    )

    public static func getStrings(for languagePref: String) -> AppStrings {
        return english // 100% English across the entire app
    }
}
