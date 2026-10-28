package com.ascdev.keyra.data

import com.ascdev.keyra.data.backup.BackupCrypto
import com.ascdev.keyra.data.backup.InvalidBackupException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCryptoTest {

    // Few iterations keep the test fast; the format stores the count, so it still round-trips.
    private val crypto = BackupCrypto(iterations = 1_000)
    private val plaintext = """{"entries":[{"title":"GitHub","password":"s3cr3t!"}]}""".toByteArray()

    @Test
    fun `round trips with the right password`() {
        val encrypted = crypto.encrypt(plaintext, "correct horse".toCharArray())
        assertArrayEquals(plaintext, crypto.decrypt(encrypted, "correct horse".toCharArray()))
    }

    @Test
    fun `can be restored by an instance with different settings`() {
        val encrypted = crypto.encrypt(plaintext, "correct horse".toCharArray())
        assertArrayEquals(plaintext, BackupCrypto().decrypt(encrypted, "correct horse".toCharArray()))
    }

    @Test
    fun `ciphertext does not contain the plaintext`() {
        val encrypted = crypto.encrypt(plaintext, "correct horse".toCharArray())
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("s3cr3t"))
    }

    @Test
    fun `two exports of the same data differ`() {
        val first = crypto.encrypt(plaintext, "pw-12345".toCharArray())
        val second = crypto.encrypt(plaintext, "pw-12345".toCharArray())
        assertFalse(first.contentEquals(second))
    }

    @Test(expected = InvalidBackupException::class)
    fun `fails with the wrong password`() {
        val encrypted = crypto.encrypt(plaintext, "correct horse".toCharArray())
        crypto.decrypt(encrypted, "wrong horse".toCharArray())
    }

    @Test(expected = InvalidBackupException::class)
    fun `fails when the file was tampered with`() {
        val encrypted = crypto.encrypt(plaintext, "correct horse".toCharArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        crypto.decrypt(encrypted, "correct horse".toCharArray())
    }

    @Test(expected = InvalidBackupException::class)
    fun `rejects files that are not Keyra backups`() {
        crypto.decrypt(ByteArray(64) { it.toByte() }, "correct horse".toCharArray())
    }
}
