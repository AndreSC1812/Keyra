package com.ascdev.keyra.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Non-sensitive user preferences. Secrets never go through this class. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            autoLockMinutes = prefs[AUTO_LOCK_MINUTES] ?: AppSettings.DEFAULT_AUTO_LOCK_MINUTES,
            lockOnBackground = prefs[LOCK_ON_BACKGROUND] ?: true,
            themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            biometricEnabled = prefs[BIOMETRIC_ENABLED] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAutoLockMinutes(minutes: Int) = dataStore.edit { it[AUTO_LOCK_MINUTES] = minutes }

    suspend fun setLockOnBackground(enabled: Boolean) = dataStore.edit { it[LOCK_ON_BACKGROUND] = enabled }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME_MODE] = mode.name }

    suspend fun setBiometricEnabled(enabled: Boolean) = dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }

    private companion object {
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val LOCK_ON_BACKGROUND = booleanPreferencesKey("lock_on_background")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }
}
