import Foundation
import Combine

public final class UserPreferencesRepository: ObservableObject {
    public static let shared = UserPreferencesRepository()

    private let defaults: UserDefaults

    public enum Keys {
        public static let highestUnlockedLevel = "highest_unlocked_level"
        public static let coins = "coins"
        public static let playerName = "player_name"
        public static let playerUid = "player_uid"
        public static let isRegistered = "is_registered"
        public static let hintBoosterCount = "hint_booster_count"
        public static let gridVisionBoosterCount = "grid_vision_booster_count"
        public static let soundEnabled = "sound_enabled"
        public static let musicEnabled = "music_enabled"
        public static let hapticEnabled = "haptic_enabled"
        public static let themeMode = "theme_mode"
        public static let language = "language"
        public static let bestScore = "best_score"
        public static let bestLevelsCleared = "best_levels_cleared"
        public static let pendingRunsJson = "pending_runs_json"

        // Active run persistence for anti-cheat
        public static let activeRunId = "active_run_id"
        public static let activeLevel = "active_level"
        public static let activeLevelHearts = "active_level_hearts"
        public static let activeAccumulatedScore = "active_accumulated_score"
        public static let activeTotalTimeMs = "active_total_time_ms"
        public static let activeLevelsCleared = "active_levels_cleared"
        public static let isLevelInProgress = "is_level_in_progress"
        public static let heartsLostInLevel = "hearts_lost_in_level"
        public static let hintsUsedInLevel = "hints_used_in_level"
        public static let gridVisionsUsedInLevel = "grid_visions_used_in_level"
        public static let currentLevelStartTimeMs = "current_level_start_time_ms"
    }

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        registerDefaults()
    }

    private func registerDefaults() {
        defaults.register(defaults: [
            Keys.highestUnlockedLevel: 1,
            Keys.coins: 150,
            Keys.playerName: "",
            Keys.playerUid: "",
            Keys.isRegistered: false,
            Keys.hintBoosterCount: 3,
            Keys.gridVisionBoosterCount: 3,
            Keys.soundEnabled: true,
            Keys.musicEnabled: true,
            Keys.hapticEnabled: true,
            Keys.themeMode: "SYSTEM",
            Keys.language: "EN",
            Keys.bestScore: 0,
            Keys.bestLevelsCleared: 0,
            Keys.pendingRunsJson: "[]",
            Keys.isLevelInProgress: false,
            Keys.activeLevelHearts: 3
        ])
    }

    public var highestUnlockedLevel: Int {
        get { defaults.integer(forKey: Keys.highestUnlockedLevel) }
        set { defaults.set(newValue, forKey: Keys.highestUnlockedLevel) }
    }

    public var coins: Int {
        get { defaults.integer(forKey: Keys.coins) }
        set { defaults.set(newValue, forKey: Keys.coins) }
    }

    public var playerName: String {
        get { defaults.string(forKey: Keys.playerName) ?? "" }
        set { defaults.set(newValue, forKey: Keys.playerName) }
    }

    public var playerUid: String {
        get { defaults.string(forKey: Keys.playerUid) ?? "" }
        set { defaults.set(newValue, forKey: Keys.playerUid) }
    }

    public var isRegistered: BooleanLiteralType {
        get { defaults.bool(forKey: Keys.isRegistered) }
        set { defaults.set(newValue, forKey: Keys.isRegistered) }
    }

    public var hintBoosterCount: Int {
        get { defaults.integer(forKey: Keys.hintBoosterCount) }
        set { defaults.set(newValue, forKey: Keys.hintBoosterCount) }
    }

    public var gridVisionBoosterCount: Int {
        get { defaults.integer(forKey: Keys.gridVisionBoosterCount) }
        set { defaults.set(newValue, forKey: Keys.gridVisionBoosterCount) }
    }

    public var bestScore: Int64 {
        get { Int64(defaults.integer(forKey: Keys.bestScore)) }
        set { defaults.set(newValue, forKey: Keys.bestScore) }
    }

    public var bestLevelsCleared: Int {
        get { defaults.integer(forKey: Keys.bestLevelsCleared) }
        set { defaults.set(newValue, forKey: Keys.bestLevelsCleared) }
    }

    public var soundEnabled: Bool {
        get { defaults.bool(forKey: Keys.soundEnabled) }
        set { defaults.set(newValue, forKey: Keys.soundEnabled) }
    }

    public var musicEnabled: Bool {
        get { defaults.bool(forKey: Keys.musicEnabled) }
        set { defaults.set(newValue, forKey: Keys.musicEnabled) }
    }

    public var hapticEnabled: Bool {
        get { defaults.bool(forKey: Keys.hapticEnabled) }
        set { defaults.set(newValue, forKey: Keys.hapticEnabled) }
    }

    public var themeMode: String {
        get { defaults.string(forKey: Keys.themeMode) ?? "SYSTEM" }
        set { defaults.set(newValue, forKey: Keys.themeMode) }
    }

    public func saveActiveRunState(_ state: ActiveRunState) {
        defaults.set(state.runId, forKey: Keys.activeRunId)
        defaults.set(state.currentLevel, forKey: Keys.activeLevel)
        defaults.set(state.levelHearts, forKey: Keys.activeLevelHearts)
        defaults.set(state.accumulatedScore, forKey: Keys.activeAccumulatedScore)
        defaults.set(state.totalTimeMs, forKey: Keys.activeTotalTimeMs)
        defaults.set(state.levelsCleared, forKey: Keys.activeLevelsCleared)
        defaults.set(state.isLevelInProgress, forKey: Keys.isLevelInProgress)
        defaults.set(state.heartsLostInLevel, forKey: Keys.heartsLostInLevel)
        defaults.set(state.hintsUsedInLevel, forKey: Keys.hintsUsedInLevel)
        defaults.set(state.gridVisionsUsedInLevel, forKey: Keys.gridVisionsUsedInLevel)
        defaults.set(state.currentLevelStartTimeMs, forKey: Keys.currentLevelStartTimeMs)
    }

    public func getActiveRunState() -> ActiveRunState {
        return ActiveRunState(
            runId: defaults.string(forKey: Keys.activeRunId) ?? UUID().uuidString,
            currentLevel: max(1, defaults.integer(forKey: Keys.activeLevel)),
            levelHearts: defaults.integer(forKey: Keys.activeLevelHearts) > 0 ? defaults.integer(forKey: Keys.activeLevelHearts) : 3,
            accumulatedScore: Int64(defaults.integer(forKey: Keys.activeAccumulatedScore)),
            totalTimeMs: Int64(defaults.integer(forKey: Keys.activeTotalTimeMs)),
            levelsCleared: defaults.integer(forKey: Keys.activeLevelsCleared),
            isLevelInProgress: defaults.bool(forKey: Keys.isLevelInProgress),
            heartsLostInLevel: defaults.integer(forKey: Keys.heartsLostInLevel),
            hintsUsedInLevel: defaults.integer(forKey: Keys.hintsUsedInLevel),
            gridVisionsUsedInLevel: defaults.integer(forKey: Keys.gridVisionsUsedInLevel),
            currentLevelStartTimeMs: Int64(defaults.integer(forKey: Keys.currentLevelStartTimeMs))
        )
    }

    public func clearActiveRunState() {
        defaults.set(false, forKey: Keys.isLevelInProgress)
        defaults.set(1, forKey: Keys.activeLevel)
        defaults.set(3, forKey: Keys.activeLevelHearts)
        defaults.set(0, forKey: Keys.activeAccumulatedScore)
        defaults.set(0, forKey: Keys.activeTotalTimeMs)
        defaults.set(0, forKey: Keys.activeLevelsCleared)
    }

    public func savePlayerRegistration(name: String, uid: String) {
        playerName = name
        playerUid = uid
        isRegistered = true
    }

    public func updateBestScore(score: Int64, levels: Int) {
        if score > bestScore {
            bestScore = score
            bestLevelsCleared = levels
        }
    }

    public func consumeHint() -> Bool {
        if hintBoosterCount > 0 {
            hintBoosterCount -= 1
            return true
        }
        return false
    }

    public func addHint(count: Int = 1) {
        hintBoosterCount += count
    }

    public func consumeGridVision() -> Bool {
        if gridVisionBoosterCount > 0 {
            gridVisionBoosterCount -= 1
            return true
        }
        return false
    }

    public func addGridVision(count: Int = 1) {
        gridVisionBoosterCount += count
    }

    public func addCoins(_ amount: Int) {
        coins += amount
    }

    public func consumeCoins(_ amount: Int) -> Bool {
        if coins >= amount {
            coins -= amount
            return true
        }
        return false
    }

    public func getPendingRuns() -> String {
        return defaults.string(forKey: Keys.pendingRunsJson) ?? "[]"
    }

    public func savePendingRuns(_ json: String) {
        defaults.set(json, forKey: Keys.pendingRunsJson)
    }
}
