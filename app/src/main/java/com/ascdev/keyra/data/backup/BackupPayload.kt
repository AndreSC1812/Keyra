package com.ascdev.keyra.data.backup

import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.domain.model.VaultEntry
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val entries: List<BackupEntry>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupEntry(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    override fun toString(): String = "BackupEntry(title=$title)"
}

fun VaultEntry.toBackup() = BackupEntry(
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BackupEntry.toDomain() = VaultEntry(
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
