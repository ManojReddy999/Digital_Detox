package com.example.digitalwellbeing.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create DataStore instance
private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

class AppPreferencesRepository(private val context: Context) {

    private val setupCompletedKey = booleanPreferencesKey("setup_completed")

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
}
