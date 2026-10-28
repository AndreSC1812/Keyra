package com.ascdev.keyra.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.domain.model.VaultEntry

@Entity(tableName = "entries", indices = [Index("category")])
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    override fun toString(): String = "EntryEntity(id=$id)"
}

fun EntryEntity.toDomain() = VaultEntry(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun VaultEntry.toEntity() = EntryEntity(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
