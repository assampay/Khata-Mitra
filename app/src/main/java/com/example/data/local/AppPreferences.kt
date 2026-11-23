package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "khatamitra_prefs")

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

data class LocalSecurityPrefs(
    val onboardingCompleted: Boolean = false,
    val pinHash: String? = null,
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val language: AppLanguage = AppLanguage.EN,
    val lastSyncMillis: Long = 0L,
    val reminderNotificationsEnabled: Boolean = true,
    val dailySummaryNotificationsEnabled: Boolean = true
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        val REMINDER_NOTIF = booleanPreferencesKey("reminder_notif")
        val SUMMARY_NOTIF = booleanPreferencesKey("summary_notif")
    }

    val prefsFlow: Flow<LocalSecurityPrefs> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemePreference.SYSTEM.name
        val langStr = prefs[Keys.LANGUAGE] ?: AppLanguage.EN.name
        LocalSecurityPrefs(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            pinHash = prefs[Keys.PIN_HASH],
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            themePreference = ThemePreference.entries.firstOrNull { it.name == themeStr }
                ?: ThemePreference.SYSTEM,
            language = AppLanguage.fromRaw(langStr),
            lastSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L,
            reminderNotificationsEnabled = prefs[Keys.REMINDER_NOTIF] ?: true,
            dailySummaryNotificationsEnabled = prefs[Keys.SUMMARY_NOTIF] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPin(pin: String) {
        val hashed = hashPin(pin)
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hashed
            it[Keys.PIN_ENABLED] = true
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it.remove(Keys.PIN_HASH)
            it[Keys.PIN_ENABLED] = false
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = theme.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateLastSyncMillis(millis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.LAST_SYNC_MILLIS] = millis }
    }

    suspend fun setNotificationSettings(reminders: Boolean, dailySummary: Boolean) {
        context.dataStore.edit {
            it[Keys.REMINDER_NOTIF] = reminders
            it[Keys.SUMMARY_NOTIF] = dailySummary
        }
    }

    fun verifyPin(rawPin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false
        return hashPin(rawPin) == storedHash
    }

    companion object {
        private const val SALT = "KhataMitra_v1_PIN_Salt"

        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$SALT:$pin".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
