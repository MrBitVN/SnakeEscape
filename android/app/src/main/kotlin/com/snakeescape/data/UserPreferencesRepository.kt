package com.snakeescape.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.snakeescape.logic.ActiveRunState
import com.snakeescape.logic.ScoringConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snake_escape_prefs")

data class UserPreferences(
    val highestUnlockedLevel: Int = 1,
    val highestUnlockedChallengeLevel: Int = 1,
    val coins: Int = 150,
    val playerName: String = "",
    val playerUid: String = "",
    val isRegistered: Boolean = false,
    val hintBoosterCount: Int = 3,
    val gridVisionBoosterCount: Int = 3,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val language: String = "EN",
    val currentGameMode: String = "CAMPAIGN",
    val bestScore: Long = 0L,
    val bestLevelsCleared: Int = 0,
    // Trạng thái Run đang chơi (Anti-cheat & Session Recovery)
    val activeRunState: ActiveRunState = ActiveRunState()
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val HIGHEST_UNLOCKED_LEVEL = intPreferencesKey("highest_unlocked_level")
        val HIGHEST_UNLOCKED_CHALLENGE_LEVEL = intPreferencesKey("highest_unlocked_challenge_level")
        val COINS = intPreferencesKey("coins")
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val PLAYER_UID = stringPreferencesKey("player_uid")
        val IS_REGISTERED = booleanPreferencesKey("is_registered")
        val HINT_BOOSTER_COUNT = intPreferencesKey("hint_booster_count")
        val GRID_VISION_BOOSTER_COUNT = intPreferencesKey("grid_vision_booster_count")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENT_GAME_MODE = stringPreferencesKey("current_game_mode")
        val BEST_SCORE = longPreferencesKey("best_score")
        val BEST_LEVELS_CLEARED = intPreferencesKey("best_levels_cleared")
        val PENDING_RUNS_JSON = stringPreferencesKey("pending_runs_json")

        // Active run persistence keys
        val ACTIVE_RUN_ID = stringPreferencesKey("active_run_id")
        val ACTIVE_LEVEL = intPreferencesKey("active_level")
        val ACTIVE_LEVEL_HEARTS = intPreferencesKey("active_level_hearts")
        val ACTIVE_ACCUMULATED_SCORE = longPreferencesKey("active_accumulated_score")
        val ACTIVE_TOTAL_TIME_MS = longPreferencesKey("active_total_time_ms")
        val ACTIVE_LEVELS_CLEARED = intPreferencesKey("active_levels_cleared")
        val IS_LEVEL_IN_PROGRESS = booleanPreferencesKey("is_level_in_progress")
        val HEARTS_LOST_IN_LEVEL = intPreferencesKey("hearts_lost_in_level")
        val HINTS_USED_IN_LEVEL = intPreferencesKey("hints_used_in_level")
        val GRID_VISIONS_USED_IN_LEVEL = intPreferencesKey("grid_visions_used_in_level")
        val LEVEL_START_TIME_MS = longPreferencesKey("level_start_time_ms")

        fun levelStarsKey(level: Int) = intPreferencesKey("level_stars_$level")
        fun challengeStarsKey(level: Int) = intPreferencesKey("challenge_stars_$level")
        fun levelBestTimeKey(level: Int) = doublePreferencesKey("level_best_time_$level")
        fun challengeBestTimeKey(level: Int) = doublePreferencesKey("challenge_best_time_$level")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val activeState = ActiveRunState(
                runId = preferences[PreferencesKeys.ACTIVE_RUN_ID] ?: "",
                currentLevel = preferences[PreferencesKeys.ACTIVE_LEVEL] ?: 1,
                levelHearts = preferences[PreferencesKeys.ACTIVE_LEVEL_HEARTS] ?: ScoringConfig.INITIAL_LEVEL_HEARTS,
                accumulatedScore = preferences[PreferencesKeys.ACTIVE_ACCUMULATED_SCORE] ?: 0L,
                totalTimeMs = preferences[PreferencesKeys.ACTIVE_TOTAL_TIME_MS] ?: 0L,
                levelsCleared = preferences[PreferencesKeys.ACTIVE_LEVELS_CLEARED] ?: 0,
                isLevelInProgress = preferences[PreferencesKeys.IS_LEVEL_IN_PROGRESS] ?: false,
                heartsLostInLevel = preferences[PreferencesKeys.HEARTS_LOST_IN_LEVEL] ?: 0,
                hintsUsedInLevel = preferences[PreferencesKeys.HINTS_USED_IN_LEVEL] ?: 0,
                gridVisionsUsedInLevel = preferences[PreferencesKeys.GRID_VISIONS_USED_IN_LEVEL] ?: 0,
                currentLevelStartTimeMs = preferences[PreferencesKeys.LEVEL_START_TIME_MS] ?: 0L
            )

            UserPreferences(
                highestUnlockedLevel = preferences[PreferencesKeys.HIGHEST_UNLOCKED_LEVEL] ?: 1,
                highestUnlockedChallengeLevel = preferences[PreferencesKeys.HIGHEST_UNLOCKED_CHALLENGE_LEVEL] ?: 1,
                coins = preferences[PreferencesKeys.COINS] ?: 150,
                playerName = preferences[PreferencesKeys.PLAYER_NAME] ?: "",
                playerUid = preferences[PreferencesKeys.PLAYER_UID] ?: "",
                isRegistered = preferences[PreferencesKeys.IS_REGISTERED] ?: false,
                hintBoosterCount = preferences[PreferencesKeys.HINT_BOOSTER_COUNT] ?: 3,
                gridVisionBoosterCount = preferences[PreferencesKeys.GRID_VISION_BOOSTER_COUNT] ?: 3,
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                musicEnabled = preferences[PreferencesKeys.MUSIC_ENABLED] ?: true,
                hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                language = "EN",
                currentGameMode = preferences[PreferencesKeys.CURRENT_GAME_MODE] ?: "CAMPAIGN",
                bestScore = preferences[PreferencesKeys.BEST_SCORE] ?: 0L,
                bestLevelsCleared = preferences[PreferencesKeys.BEST_LEVELS_CLEARED] ?: 0,
                activeRunState = activeState
            )
        }

    suspend fun saveActiveRunState(state: ActiveRunState) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_RUN_ID] = state.runId
            preferences[PreferencesKeys.ACTIVE_LEVEL] = state.currentLevel
            preferences[PreferencesKeys.ACTIVE_LEVEL_HEARTS] = state.levelHearts
            preferences[PreferencesKeys.ACTIVE_ACCUMULATED_SCORE] = state.accumulatedScore
            preferences[PreferencesKeys.ACTIVE_TOTAL_TIME_MS] = state.totalTimeMs
            preferences[PreferencesKeys.ACTIVE_LEVELS_CLEARED] = state.levelsCleared
            preferences[PreferencesKeys.IS_LEVEL_IN_PROGRESS] = state.isLevelInProgress
            preferences[PreferencesKeys.HEARTS_LOST_IN_LEVEL] = state.heartsLostInLevel
            preferences[PreferencesKeys.HINTS_USED_IN_LEVEL] = state.hintsUsedInLevel
            preferences[PreferencesKeys.GRID_VISIONS_USED_IN_LEVEL] = state.gridVisionsUsedInLevel
            preferences[PreferencesKeys.LEVEL_START_TIME_MS] = state.currentLevelStartTimeMs
        }
    }

    suspend fun clearActiveRunState() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LEVEL_IN_PROGRESS] = false
            preferences[PreferencesKeys.ACTIVE_LEVEL] = 1
            preferences[PreferencesKeys.ACTIVE_LEVEL_HEARTS] = ScoringConfig.INITIAL_LEVEL_HEARTS
            preferences[PreferencesKeys.ACTIVE_ACCUMULATED_SCORE] = 0L
            preferences[PreferencesKeys.ACTIVE_TOTAL_TIME_MS] = 0L
            preferences[PreferencesKeys.ACTIVE_LEVELS_CLEARED] = 0
            preferences[PreferencesKeys.HEARTS_LOST_IN_LEVEL] = 0
            preferences[PreferencesKeys.HINTS_USED_IN_LEVEL] = 0
            preferences[PreferencesKeys.GRID_VISIONS_USED_IN_LEVEL] = 0
        }
    }

    suspend fun savePlayerRegistration(name: String, uid: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_NAME] = name
            preferences[PreferencesKeys.PLAYER_UID] = uid
            preferences[PreferencesKeys.IS_REGISTERED] = true
        }
    }

    suspend fun updateBestScore(score: Long, levelsCleared: Int) {
        context.dataStore.edit { preferences ->
            val curBest = preferences[PreferencesKeys.BEST_SCORE] ?: 0L
            if (score > curBest) {
                preferences[PreferencesKeys.BEST_SCORE] = score
                preferences[PreferencesKeys.BEST_LEVELS_CLEARED] = levelsCleared
            }
        }
    }

    suspend fun getPendingRuns(): String {
        val prefs = context.dataStore.data.first()
        return prefs[PreferencesKeys.PENDING_RUNS_JSON] ?: "[]"
    }

    suspend fun savePendingRuns(json: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PENDING_RUNS_JSON] = json
        }
    }

    suspend fun unlockLevel(nextLevel: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIGHEST_UNLOCKED_LEVEL] ?: 1
            if (nextLevel > current) {
                preferences[PreferencesKeys.HIGHEST_UNLOCKED_LEVEL] = nextLevel
            }
        }
    }

    suspend fun unlockChallengeLevel(nextLevel: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIGHEST_UNLOCKED_CHALLENGE_LEVEL] ?: 1
            if (nextLevel > current) {
                preferences[PreferencesKeys.HIGHEST_UNLOCKED_CHALLENGE_LEVEL] = nextLevel
            }
        }
    }

    suspend fun setCurrentGameMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_GAME_MODE] = mode
        }
    }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 150
            preferences[PreferencesKeys.COINS] = current + amount
        }
    }

    suspend fun consumeCoins(amount: Int): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.COINS] ?: 150
            if (current >= amount) {
                preferences[PreferencesKeys.COINS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun saveLevelStars(level: Int, stars: Int, isChallenge: Boolean = false) {
        context.dataStore.edit { preferences ->
            val key = if (isChallenge) PreferencesKeys.challengeStarsKey(level) else PreferencesKeys.levelStarsKey(level)
            val current = preferences[key] ?: 0
            if (stars > current) {
                preferences[key] = stars
            }
        }
    }

    suspend fun saveLevelBestTime(level: Int, timeSeconds: Double, isChallenge: Boolean = false) {
        context.dataStore.edit { preferences ->
            val key = if (isChallenge) PreferencesKeys.challengeBestTimeKey(level) else PreferencesKeys.levelBestTimeKey(level)
            val current = preferences[key] ?: Double.MAX_VALUE
            if (timeSeconds < current) {
                preferences[key] = timeSeconds
            }
        }
    }

    suspend fun consumeHint(): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HINT_BOOSTER_COUNT] ?: 3
            if (current > 0) {
                preferences[PreferencesKeys.HINT_BOOSTER_COUNT] = current - 1
                success = true
            }
        }
        return success
    }

    suspend fun addHint(count: Int = 1) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HINT_BOOSTER_COUNT] ?: 3
            preferences[PreferencesKeys.HINT_BOOSTER_COUNT] = current + count
        }
    }

    suspend fun consumeGridVision(): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.GRID_VISION_BOOSTER_COUNT] ?: 3
            if (current > 0) {
                preferences[PreferencesKeys.GRID_VISION_BOOSTER_COUNT] = current - 1
                success = true
            }
        }
        return success
    }

    suspend fun addGridVision(count: Int = 1) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.GRID_VISION_BOOSTER_COUNT] ?: 3
            preferences[PreferencesKeys.GRID_VISION_BOOSTER_COUNT] = current + count
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = "EN"
        }
    }
}
