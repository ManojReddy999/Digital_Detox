package com.focus.digitalwellbeing.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create DataStore instance
private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromString(value: String): AppTheme {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                SYSTEM
            }
        }
    }
}

class AppPreferencesRepository(private val context: Context) {

    private val setupCompletedKey = booleanPreferencesKey("setup_completed")
    private val themeKey = stringPreferencesKey("app_theme")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userProfileUriKey = stringPreferencesKey("user_profile_uri")

    /**
     * Get setup completion status
     */
    fun isSetupCompleted(): Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[setupCompletedKey] ?: false
    }

    /**
     * Mark setup as completed
     */
    suspend fun markSetupCompleted() {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[setupCompletedKey] = true
        }
    }

    /**
     * Reset setup completion (for testing)
     */
    suspend fun resetSetup() {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[setupCompletedKey] = false
        }
    }

    /**
     * Get current theme preference
     */
    fun getTheme(): Flow<AppTheme> = context.appPreferencesDataStore.data.map { prefs ->
        AppTheme.fromString(prefs[themeKey] ?: AppTheme.SYSTEM.name)
    }

    /**
     * Save theme preference
     */
    suspend fun setTheme(theme: AppTheme) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }
    /**
     * Save user profile
     */
    suspend fun saveUserProfile(name: String, profileUri: String?) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[userNameKey] = name
            if (profileUri != null) {
                prefs[userProfileUriKey] = profileUri
            } else {
                prefs.remove(userProfileUriKey)
            }
        }
    }

    /**
     * Get user name
     */
    fun getUserName(): Flow<String?> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[userNameKey]
    }

    /**
     * Get user profile URI
     */
    fun getUserProfileUri(): Flow<String?> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[userProfileUriKey]
    }
}

