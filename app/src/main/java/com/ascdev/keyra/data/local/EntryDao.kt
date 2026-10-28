package com.ascdev.keyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<EntryEntity?>

    @Query("SELECT * FROM entries ORDER BY title COLLATE NOCASE")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<EntryEntity>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
