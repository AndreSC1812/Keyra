package com.ascdev.keyra.data.backup

import android.content.Context
import android.net.Uri
import com.ascdev.keyra.core.di.IoDispatcher
import com.ascdev.keyra.domain.repository.EntryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: EntryRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val crypto = BackupCrypto()
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns the number of exported entries. */
    suspend fun export(uri: Uri, password: CharArray): Int = withContext(dispatcher) {
        val entries = repository.getEntries()
        val payload = BackupPayload(exportedAt = System.currentTimeMillis(), entries = entries.map { it.toBackup() })
        val plaintext = json.encodeToString(BackupPayload.serializer(), payload).toByteArray()
        val encrypted = crypto.encrypt(plaintext, password)
        plaintext.fill(0)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(encrypted) }
            ?: throw InvalidBackupException("Cannot write the selected file")
        entries.size
    }

    /** Imports the entries that are not already in the vault and returns how many were added. */
    suspend fun import(uri: Uri, password: CharArray): Int = withContext(dispatcher) {
        val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw InvalidBackupException("Cannot read the selected file")
        val plaintext = crypto.decrypt(encrypted, password)
        val payload = try {
            json.decodeFromString(BackupPayload.serializer(), plaintext.decodeToString())
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException("Corrupted backup", e)
        } finally {
            plaintext.fill(0)
        }

        val existing = repository.getEntries().mapTo(HashSet()) { Triple(it.title, it.username, it.password) }
        val newEntries = payload.entries
            .map { it.toDomain() }
            .filter { Triple(it.title, it.username, it.password) !in existing }
        repository.saveAll(newEntries)
        newEntries.size
    }
}
