package com.ascdev.keyra.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the two hardware-backed AES keys of the app:
 *  - the biometric key, which only works after a strong biometric authentication and is
 *    invalidated as soon as a new fingerprint is enrolled;
 *  - the device key, which binds the PIN-wrapped vault key to this device so it cannot be
 *    brute forced elsewhere.
 * Neither key ever leaves the Android Keystore.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Cipher that BiometricPrompt has to unlock before it can encrypt the vault key. */
    fun biometricEncryptCipher(): Cipher {
        deleteKey(BIOMETRIC_KEY_ALIAS)
        return Cipher.getInstance(AesGcm.TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, createBiometricKey())
        }
    }

    /**
     * Cipher that BiometricPrompt has to unlock before it can decrypt the vault key.
     * Returns null when the key has been invalidated by a new biometric enrollment.
     */
    fun biometricDecryptCipher(iv: ByteArray): Cipher? {
        val key = getKey(BIOMETRIC_KEY_ALIAS) ?: return null
        return try {
            Cipher.getInstance(AesGcm.TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AesGcm.TAG_BITS, iv))
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            deleteKey(BIOMETRIC_KEY_ALIAS)
            null
        }
    }

    fun deleteBiometricKey() = deleteKey(BIOMETRIC_KEY_ALIAS)

    fun encryptWithDeviceKey(plaintext: ByteArray): ByteArray =
        AesGcm.encrypt(getKey(DEVICE_KEY_ALIAS) ?: createDeviceKey(), plaintext)

    fun decryptWithDeviceKey(data: ByteArray): ByteArray {
        val key = checkNotNull(getKey(DEVICE_KEY_ALIAS)) { "Device key is missing" }
        return AesGcm.decrypt(key, data)
    }

    private fun createBiometricKey(): SecretKey {
        val builder = baseSpec(BIOMETRIC_KEY_ALIAS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        return generate(builder.build())
    }

    private fun createDeviceKey(): SecretKey = generate(baseSpec(DEVICE_KEY_ALIAS).build())

    private fun baseSpec(alias: String) = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(AesGcm.KEY_BITS)
        .setRandomizedEncryptionRequired(true)

    private fun generate(spec: KeyGenParameterSpec): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }

    private fun getKey(alias: String): SecretKey? = keyStore.getKey(alias, null) as? SecretKey

    private fun deleteKey(alias: String) {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val BIOMETRIC_KEY_ALIAS = "keyra_biometric_key"
        const val DEVICE_KEY_ALIAS = "keyra_device_key"
    }
}
