package com.ascdev.keyra.core.session

import com.ascdev.keyra.data.local.VaultDatabase
import com.ascdev.keyra.data.local.VaultDatabaseFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

class VaultLockedException : IllegalStateException("The vault is locked")

/**
 * Holds the decrypted vault while it is unlocked. Locking closes the database and wipes the
 * vault key from memory, so nothing sensitive survives until the next unlock.
 */
@Singleton
class VaultSession @Inject constructor(
    private val databaseFactory: VaultDatabaseFactory,
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var database: VaultDatabase? = null
    private var vaultKey: ByteArray? = null

    @Synchronized
    fun open(key: ByteArray) {
        lock()
        database = databaseFactory.open(key)
        vaultKey = key.copyOf()
        key.fill(0)
        _isUnlocked.value = true
    }

    @Synchronized
    fun lock() {
        _isUnlocked.value = false
        database?.close()
        database = null
        vaultKey?.fill(0)
        vaultKey = null
    }

    @Synchronized
    fun requireDatabase(): VaultDatabase = database ?: throw VaultLockedException()

    /** Gives temporary access to the vault key, e.g. to enable biometric unlock. */
    @Synchronized
    fun <T> withVaultKey(block: (ByteArray) -> T): T {
        val key = vaultKey ?: throw VaultLockedException()
        return block(key)
    }
}
