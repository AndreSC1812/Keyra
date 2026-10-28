package com.ascdev.keyra.domain.repository

import com.ascdev.keyra.domain.model.VaultEntry
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun observeEntries(): Flow<List<VaultEntry>>
    fun observeEntry(id: Long): Flow<VaultEntry?>
    suspend fun getEntries(): List<VaultEntry>
    suspend fun getEntry(id: Long): VaultEntry?
    suspend fun save(entry: VaultEntry): Long
    suspend fun saveAll(entries: List<VaultEntry>)
    suspend fun delete(id: Long)
}
