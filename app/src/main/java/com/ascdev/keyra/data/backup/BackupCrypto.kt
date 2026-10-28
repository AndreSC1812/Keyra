package com.ascdev.keyra.data.backup

import com.ascdev.keyra.core.security.AesGcm
import java.nio.ByteBuffer
import java.security.GeneralSecurityException

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Encrypted backup file format:
 *
 * | magic "KEYRA" | version (1 byte) | iterations (4 bytes) | salt (16) | iv (12) | ciphertext + GCM tag |
 *
 * The key is derived from the backup password with PBKDF2-HMAC-SHA256, so the file can be
 * restored on any device, unlike the vault itself which is bound to this device's Keystore.
 */
class BackupCrypto(private val iterations: Int = DEFAULT_ITERATIONS) {

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = AesGcm.randomBytes(AesGcm.SALT_SIZE)
        val key = AesGcm.deriveKey(password, salt, iterations)
        val encrypted = AesGcm.encrypt(key, plaintext)
        return ByteBuffer.allocate(HEADER_SIZE + encrypted.size)
            .put(MAGIC)
            .put(FORMAT_VERSION)
            .putInt(iterations)
            .put(salt)
            .put(encrypted)
            .array()
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (data.size <= HEADER_SIZE + AesGcm.IV_SIZE) throw InvalidBackupException("File is too short")
        val buffer = ByteBuffer.wrap(data)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        if (!magic.contentEquals(MAGIC)) throw InvalidBackupException("Not a Keyra backup")
        val version = buffer.get()
        if (version != FORMAT_VERSION) throw InvalidBackupException("Unsupported backup version $version")
        val fileIterations = buffer.getInt()
        if (fileIterations !in 1..MAX_ITERATIONS) throw InvalidBackupException("Corrupted header")
        val salt = ByteArray(AesGcm.SALT_SIZE).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)

        return try {
            AesGcm.decrypt(AesGcm.deriveKey(password, salt, fileIterations), encrypted)
        } catch (e: GeneralSecurityException) {
            throw InvalidBackupException("Wrong password or corrupted file", e)
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 210_000
        private const val MAX_ITERATIONS = 10_000_000
        private val MAGIC = "KEYRA".toByteArray(Charsets.US_ASCII)
        private const val FORMAT_VERSION: Byte = 1
        private val HEADER_SIZE = MAGIC.size + 1 + Int.SIZE_BYTES + AesGcm.SALT_SIZE
    }
}
