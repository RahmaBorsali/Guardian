package com.guardian.track.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences manager for GuardianTrack user settings.
 * Stores: sensitivity threshold, dark mode, emergency number, SMS simulation mode.
 * Note: Emergency number is stored encrypted via SecurePreferences separately.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        val KEY_EMERGENCY_NUMBER = stringPreferencesKey("emergency_number")
        val KEY_SMS_SIMULATION = booleanPreferencesKey("sms_simulation_enabled")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")

        const val DEFAULT_SENSITIVITY_THRESHOLD = 15.0f
        const val DEFAULT_SMS_SIMULATION = true
        const val DEFAULT_DARK_MODE = true
        const val DEFAULT_SERVICE_ENABLED = false
    }

    // Sensitivity threshold (default: 15.0 m/s²)
    val sensitivityThreshold: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_SENSITIVITY_THRESHOLD] ?: DEFAULT_SENSITIVITY_THRESHOLD
    }

    suspend fun setSensitivityThreshold(value: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY_THRESHOLD] = value
        }
    }

    // Dark mode
    val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: DEFAULT_DARK_MODE
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    // Emergency number (plain text in DataStore; encrypted copy in SecurePreferences)
    val emergencyNumber: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_EMERGENCY_NUMBER] ?: ""
    }

    suspend fun setEmergencyNumber(number: String) {
        dataStore.edit { prefs ->
            prefs[KEY_EMERGENCY_NUMBER] = number
        }
    }

    // SMS simulation mode (default: ACTIVE as required by spec)
    val isSmsSimulation: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SMS_SIMULATION] ?: DEFAULT_SMS_SIMULATION
    }

    suspend fun setSmsSimulation(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SMS_SIMULATION] = enabled
        }
    }

    // Service enabled state
    val isServiceEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SERVICE_ENABLED] ?: DEFAULT_SERVICE_ENABLED
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SERVICE_ENABLED] = enabled
        }
    }
}
