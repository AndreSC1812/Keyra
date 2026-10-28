package com.ascdev.keyra.core.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the wrapped (encrypted) copies of the vault key and the PIN attempt counter. */
@Singleton
class VaultKeyStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val isSetupComplete: Flow<Boolean> = dataStore.data.map { it[SETUP_COMPLETE] ?: false }

    suspend fun saveAfterPinWrap(salt: ByteArray, wrappedKey: ByteArray) = dataStore.edit {
        it[PIN_SALT] = salt.encode()
        it[PIN_WRAPPED_KEY] = wrappedKey.encode()
        it.remove(BIOMETRIC_WRAPPED_KEY)
        it[FAILED_ATTEMPTS] = 0
        it[LOCKOUT_UNTIL] = 0L
        it[SETUP_COMPLETE] = false
    }

    suspend fun markSetupComplete() = dataStore.edit { it[SETUP_COMPLETE] = true }

    suspend fun pinSalt(): ByteArray? = read(PIN_SALT)

    suspend fun pinWrappedKey(): ByteArray? = read(PIN_WRAPPED_KEY)

    suspend fun biometricWrappedKey(): ByteArray? = read(BIOMETRIC_WRAPPED_KEY)

    suspend fun saveBiometricWrappedKey(data: ByteArray) =
        dataStore.edit { it[BIOMETRIC_WRAPPED_KEY] = data.encode() }

    suspend fun clearBiometricWrappedKey() = dataStore.edit { it.remove(BIOMETRIC_WRAPPED_KEY) }

    suspend fun failedAttempts(): Int = dataStore.data.first()[FAILED_ATTEMPTS] ?: 0

    suspend fun lockoutUntil(): Long = dataStore.data.first()[LOCKOUT_UNTIL] ?: 0L

    suspend fun recordFailedAttempt(lockoutUntil: Long) = dataStore.edit {
        it[FAILED_ATTEMPTS] = (it[FAILED_ATTEMPTS] ?: 0) + 1
        it[LOCKOUT_UNTIL] = lockoutUntil
    }

    suspend fun resetFailedAttempts() = dataStore.edit {
        it[FAILED_ATTEMPTS] = 0
        it[LOCKOUT_UNTIL] = 0L
    }

    private suspend fun read(key: Preferences.Key<String>): ByteArray? =
        dataStore.data.first()[key]?.let { Base64.decode(it, Base64.NO_WRAP) }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_WRAPPED_KEY = stringPreferencesKey("pin_wrapped_key")
        val BIOMETRIC_WRAPPED_KEY = stringPreferencesKey("biometric_wrapped_key")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
    }
}
