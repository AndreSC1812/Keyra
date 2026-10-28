package com.ascdev.keyra.core.security

import com.ascdev.keyra.core.di.IoDispatcher
import com.ascdev.keyra.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinUnlockResult {
    class Success(val vaultKey: ByteArray) : PinUnlockResult
    data class WrongPin(val attemptsLeft: Int, val lockoutUntil: Long) : PinUnlockResult
    data class LockedOut(val until: Long) : PinUnlockResult
}

/**
 * Creates and unwraps the random 256-bit vault key that encrypts the SQLCipher database.
 *
 * The vault key is stored twice, never in plain text:
 *  - PIN path: AES-GCM with a PBKDF2 key derived from the PIN, then wrapped again with the
 *    device Keystore key.
 *  - Biometric path: AES-GCM with the biometric Keystore key, which BiometricPrompt has to
 *    unlock through a CryptoObject.
 */
@Singleton
class VaultKeyManager @Inject constructor(
    private val keystore: KeystoreManager,
    private val keyStore: VaultKeyStore,
    private val settings: SettingsRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    val isSetupComplete = keyStore.isSetupComplete

    suspend fun createVault(pin: CharArray): ByteArray = withContext(dispatcher) {
        val vaultKey = AesGcm.randomBytes(VAULT_KEY_SIZE)
        val salt = AesGcm.randomBytes(AesGcm.SALT_SIZE)
        val pinKey = AesGcm.deriveKey(pin, salt, PIN_ITERATIONS)
        val wrapped = keystore.encryptWithDeviceKey(AesGcm.encrypt(pinKey, vaultKey))
        keystore.deleteBiometricKey()
        keyStore.saveAfterPinWrap(salt, wrapped)
        settings.setBiometricEnabled(false)
        vaultKey
    }

    suspend fun completeSetup() = keyStore.markSetupComplete()

    suspend fun unlockWithPin(pin: CharArray, now: Long = System.currentTimeMillis()): PinUnlockResult =
        withContext(dispatcher) {
            val lockoutUntil = keyStore.lockoutUntil()
            if (lockoutUntil > now) return@withContext PinUnlockResult.LockedOut(lockoutUntil)

            val salt = checkNotNull(keyStore.pinSalt())
            val wrapped = checkNotNull(keyStore.pinWrappedKey())
            try {
                val pinKey = AesGcm.deriveKey(pin, salt, PIN_ITERATIONS)
                val vaultKey = AesGcm.decrypt(pinKey, keystore.decryptWithDeviceKey(wrapped))
                keyStore.resetFailedAttempts()
                PinUnlockResult.Success(vaultKey)
            } catch (_: GeneralSecurityException) {
                val failed = keyStore.failedAttempts() + 1
                val until = now + PinLockoutPolicy.lockoutDurationMs(failed)
                keyStore.recordFailedAttempt(if (until > now) until else 0L)
                PinUnlockResult.WrongPin(
                    attemptsLeft = (PinLockoutPolicy.FREE_ATTEMPTS - failed).coerceAtLeast(0),
                    lockoutUntil = until,
                )
            }
        }

    suspend fun currentLockoutUntil(): Long = keyStore.lockoutUntil()

    fun biometricEncryptCipher(): Cipher = keystore.biometricEncryptCipher()

    /** Null when biometrics are off or the key was invalidated by a new enrollment. */
    suspend fun biometricDecryptCipher(): Cipher? {
        val wrapped = keyStore.biometricWrappedKey() ?: return null
        val cipher = keystore.biometricDecryptCipher(AesGcm.unpack(wrapped).first)
        if (cipher == null) disableBiometric()
        return cipher
    }

    /** [cipher] must have been authenticated by BiometricPrompt. */
    suspend fun enableBiometric(cipher: Cipher, vaultKey: ByteArray) {
        val ciphertext = cipher.doFinal(vaultKey)
        keyStore.saveBiometricWrappedKey(AesGcm.pack(cipher.iv, ciphertext))
        settings.setBiometricEnabled(true)
    }

    /** [cipher] must have been authenticated by BiometricPrompt. */
    suspend fun unlockWithBiometric(cipher: Cipher): ByteArray {
        val wrapped = checkNotNull(keyStore.biometricWrappedKey())
        val vaultKey = cipher.doFinal(AesGcm.unpack(wrapped).second)
        keyStore.resetFailedAttempts()
        return vaultKey
    }

    suspend fun disableBiometric() {
        keystore.deleteBiometricKey()
        keyStore.clearBiometricWrappedKey()
        settings.setBiometricEnabled(false)
    }

    private companion object {
        const val VAULT_KEY_SIZE = 32
        const val PIN_ITERATIONS = 120_000
    }
}
