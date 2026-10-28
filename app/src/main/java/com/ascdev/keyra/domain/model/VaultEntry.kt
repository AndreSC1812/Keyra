package com.ascdev.keyra.domain.model

data class VaultEntry(
    val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: Category,
    val createdAt: Long,
    val updatedAt: Long,
) {
    // Keeps passwords out of logs and crash reports if an entry is ever printed.
    override fun toString(): String = "VaultEntry(id=$id, title=$title, category=$category)"
}
