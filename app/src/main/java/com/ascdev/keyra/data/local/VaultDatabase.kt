package com.ascdev.keyra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntryEntity::class], version = 1, exportSchema = true)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        const val NAME = "keyra_vault.db"
    }
}
