package com.ascdev.keyra.data.repository

import com.ascdev.keyra.core.di.IoDispatcher
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.data.local.EntryDao
import com.ascdev.keyra.data.local.toDomain
import com.ascdev.keyra.data.local.toEntity
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomEntryRepository @Inject constructor(
    private val session: VaultSession,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : EntryRepository {

    private val dao: EntryDao get() = session.requireDatabase().entryDao()

    // Every stream switches to empty as soon as the vault is locked.
    override fun observeEntries(): Flow<List<VaultEntry>> = session.isUnlocked.flatMapLatest { unlocked ->
        if (!unlocked) {
            flowOf(emptyList())
        } else {
            dao.observeAll().map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }
        }
    }

    override fun observeEntry(id: Long): Flow<VaultEntry?> = session.isUnlocked.flatMapLatest { unlocked ->
        if (!unlocked) flowOf(null) else dao.observeById(id).map { it?.toDomain() }.catch { emit(null) }
    }

    override suspend fun getEntries(): List<VaultEntry> = withContext(dispatcher) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun getEntry(id: Long): VaultEntry? = withContext(dispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun save(entry: VaultEntry): Long = withContext(dispatcher) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun saveAll(entries: List<VaultEntry>) = withContext(dispatcher) {
        dao.insertAll(entries.map { it.toEntity().copy(id = 0) })
    }

    override suspend fun delete(id: Long) = withContext(dispatcher) {
        dao.deleteById(id)
    }
}
