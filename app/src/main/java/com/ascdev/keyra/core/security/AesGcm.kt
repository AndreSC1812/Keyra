package com.ascdev.keyra.core.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Pure JVM crypto helpers shared by the PIN wrapping and the encrypted backups. */
object AesGcm {
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val IV_SIZE = 12
    const val TAG_BITS = 128
    const val KEY_BITS = 256
    const val SALT_SIZE = 16

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    fun deriveKey(secret: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(secret, salt, iterations, KEY_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** Returns iv + ciphertext. */
    fun encrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return pack(cipher.iv, cipher.doFinal(plaintext))
    }

    /** Expects iv + ciphertext, as produced by [encrypt]. Throws if the key is wrong. */
    fun decrypt(key: SecretKey, data: ByteArray): ByteArray {
        val (iv, ciphertext) = unpack(data)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun pack(iv: ByteArray, ciphertext: ByteArray): ByteArray =
        ByteBuffer.allocate(iv.size + ciphertext.size).put(iv).put(ciphertext).array()

    fun unpack(data: ByteArray): Pair<ByteArray, ByteArray> {
        require(data.size > IV_SIZE) { "Encrypted data is too short" }
        return data.copyOfRange(0, IV_SIZE) to data.copyOfRange(IV_SIZE, data.size)
    }
}
