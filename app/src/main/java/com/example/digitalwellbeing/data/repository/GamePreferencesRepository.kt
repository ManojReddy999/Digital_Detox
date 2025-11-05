package com.example.digitalwellbeing.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.digitalwellbeing.data.model.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gamePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_preferences")

/**
 * Repository for managing game preferences and bonus time
 */
class GamePreferencesRepository(private val context: Context) {

    // Keys for enabled games
    private val sudokuEnabledKey = booleanPreferencesKey("game_sudoku_enabled")
    private val mathPuzzleEnabledKey = booleanPreferencesKey("game_math_puzzle_enabled")
    private val chessMoveEnabledKey = booleanPreferencesKey("game_chess_move_enabled")
    private val mathQuestionEnabledKey = booleanPreferencesKey("game_math_question_enabled")
    private val riddleEnabledKey = booleanPreferencesKey("game_riddle_enabled")
    private val jigsawEnabledKey = booleanPreferencesKey("game_jigsaw_enabled")
    private val probabilityEnabledKey = booleanPreferencesKey("game_probability_enabled")
    private val logicPuzzleEnabledKey = booleanPreferencesKey("game_logic_puzzle_enabled")
    private val initializedKey = booleanPreferencesKey("games_initialized")

    // Keys for bonus time (per package name)
    private fun getBonusTimeKey(packageName: String) = longPreferencesKey("bonus_time_$packageName")
    private fun getBonusExpiryKey(packageName: String) = longPreferencesKey("bonus_expiry_$packageName")

    /**
     * Get all enabled games
     */
    fun getEnabledGames(): Flow<Set<GameType>> = context.gamePreferencesDataStore.data.map { prefs ->
        // Build enabled set from preferences (default is disabled)
        val enabled = mutableSetOf<GameType>()

        if (prefs[sudokuEnabledKey] == true) enabled.add(GameType.SUDOKU)
        if (prefs[mathPuzzleEnabledKey] == true) enabled.add(GameType.MATH_PUZZLE)
        if (prefs[chessMoveEnabledKey] == true) enabled.add(GameType.CHESS_MOVE)
        if (prefs[mathQuestionEnabledKey] == true) enabled.add(GameType.MATH_QUESTION)
        if (prefs[riddleEnabledKey] == true) enabled.add(GameType.RIDDLE)
        if (prefs[jigsawEnabledKey] == true) enabled.add(GameType.JIGSAW_PUZZLE)
        if (prefs[probabilityEnabledKey] == true) enabled.add(GameType.PROBABILITY)
        if (prefs[logicPuzzleEnabledKey] == true) enabled.add(GameType.LOGIC_PUZZLE)

        enabled
    }

    /**
     * Check if a specific game is enabled
     */
    fun isGameEnabled(gameType: GameType): Flow<Boolean> = context.gamePreferencesDataStore.data.map { prefs ->
        val key = when (gameType) {
            GameType.SUDOKU -> sudokuEnabledKey
            GameType.MATH_PUZZLE -> mathPuzzleEnabledKey
            GameType.CHESS_MOVE -> chessMoveEnabledKey
            GameType.MATH_QUESTION -> mathQuestionEnabledKey
            GameType.RIDDLE -> riddleEnabledKey
            GameType.JIGSAW_PUZZLE -> jigsawEnabledKey
            GameType.PROBABILITY -> probabilityEnabledKey
            GameType.LOGIC_PUZZLE -> logicPuzzleEnabledKey
        }
        prefs[key] ?: false // Default to disabled
    }

    /**
     * Set whether a game is enabled
     */
    suspend fun setGameEnabled(gameType: GameType, enabled: Boolean) {
        val key = when (gameType) {
            GameType.SUDOKU -> sudokuEnabledKey
            GameType.MATH_PUZZLE -> mathPuzzleEnabledKey
            GameType.CHESS_MOVE -> chessMoveEnabledKey
            GameType.MATH_QUESTION -> mathQuestionEnabledKey
            GameType.RIDDLE -> riddleEnabledKey
            GameType.JIGSAW_PUZZLE -> jigsawEnabledKey
            GameType.PROBABILITY -> probabilityEnabledKey
            GameType.LOGIC_PUZZLE -> logicPuzzleEnabledKey
        }

        context.gamePreferencesDataStore.edit { prefs ->
            prefs[key] = enabled
            android.util.Log.d("GamePreferences", "Set ${gameType.displayName} to $enabled")
        }
    }

    /**
     * Get available bonus time for a specific app (in milliseconds)
     */
    fun getBonusTime(packageName: String): Flow<Long> = context.gamePreferencesDataStore.data.map { prefs ->
        val bonusTime = prefs[getBonusTimeKey(packageName)] ?: 0L
        val expiry = prefs[getBonusExpiryKey(packageName)] ?: 0L

        // Check if bonus has expired
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            0L  // Bonus expired
        } else {
            bonusTime
        }
    }

    /**
     * Add bonus time for an app (in minutes)
     */
    suspend fun addBonusTime(packageName: String, bonusMinutes: Int) {
        context.gamePreferencesDataStore.edit { prefs ->
            val key = getBonusTimeKey(packageName)
            val expiryKey = getBonusExpiryKey(packageName)

            val currentBonus = prefs[key] ?: 0L
            val bonusMillis = bonusMinutes * 60 * 1000L

            prefs[key] = currentBonus + bonusMillis

            // Set expiry to end of today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            prefs[expiryKey] = calendar.timeInMillis
        }
    }

    /**
     * Use bonus time (deduct from available)
     */
    suspend fun useBonusTime(packageName: String, usedMillis: Long) {
        context.gamePreferencesDataStore.edit { prefs ->
            val key = getBonusTimeKey(packageName)
            val currentBonus = prefs[key] ?: 0L
            prefs[key] = (currentBonus - usedMillis).coerceAtLeast(0L)
        }
    }

    /**
     * Clear all bonus time (called at midnight)
     */
    suspend fun clearExpiredBonuses() {
        context.gamePreferencesDataStore.edit { prefs ->
            val now = System.currentTimeMillis()

            // Find all expiry keys and clear expired bonuses
            prefs.asMap().keys.forEach { key ->
                if (key.name.startsWith("bonus_expiry_")) {
                    val expiry = prefs[key as Preferences.Key<Long>] ?: 0L
                    if (now > expiry) {
                        val packageName = key.name.removePrefix("bonus_expiry_")
                        prefs.remove(getBonusTimeKey(packageName))
                        prefs.remove(key)
                    }
                }
            }
        }
    }
}
