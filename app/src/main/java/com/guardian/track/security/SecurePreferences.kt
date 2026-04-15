package com.guardian.track.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data using EncryptedSharedPreferences.
 * Encrypts the emergency phone number and API key locally.
 * Uses AES256_GCM for value encryption and AES256_SIV for key encryption.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "guardian_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_EMERGENCY_NUMBER = "encrypted_emergency_number"
        private const val KEY_API_KEY = "encrypted_api_key"
    }

    /**
     * Store the emergency phone number encrypted.
     */
    fun setEmergencyNumber(number: String) {
        securePrefs.edit().putString(KEY_EMERGENCY_NUMBER, number).apply()
    }

    /**
     * Retrieve the decrypted emergency phone number.
     */
    fun getEmergencyNumber(): String {
        return securePrefs.getString(KEY_EMERGENCY_NUMBER, "") ?: ""
    }

    /**
     * Store the API key encrypted.
     */
    fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    /**
     * Retrieve the decrypted API key.
     */
    fun getApiKey(): String {
        return securePrefs.getString(KEY_API_KEY, "") ?: ""
    }
}
