package com.ascdev.keyra.ui

import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEntryRepository(initial: List<VaultEntry> = emptyList()) : EntryRepository {
    val entries = MutableStateFlow(initial)

    override fun observeEntries(): Flow<List<VaultEntry>> = entries
    override fun observeEntry(id: Long): Flow<VaultEntry?> = entries.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getEntries(): List<VaultEntry> = entries.value
    override suspend fun getEntry(id: Long): VaultEntry? = entries.value.firstOrNull { it.id == id }

    override suspend fun save(entry: VaultEntry): Long {
        val id = if (entry.id == 0L) (entries.value.maxOfOrNull { it.id } ?: 0) + 1 else entry.id
        entries.value = entries.value.filterNot { it.id == id } + entry.copy(id = id)
        return id
    }

    override suspend fun saveAll(entries: List<VaultEntry>) = entries.forEach { save(it.copy(id = 0)) }

    override suspend fun delete(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}
