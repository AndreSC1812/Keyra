package com.ascdev.keyra.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/** Opens the SQLCipher database with the raw vault key, so no extra KDF runs on every unlock. */
@Singleton
class VaultDatabaseFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun open(vaultKey: ByteArray): VaultDatabase {
        val passphrase = rawKeyPassphrase(vaultKey)
        return Room.databaseBuilder(context, VaultDatabase::class.java, VaultDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }

    fun delete() {
        context.deleteDatabase(VaultDatabase.NAME)
    }

    // SQLCipher treats x'<hex>' as a raw 256-bit key instead of a passphrase.
    private fun rawKeyPassphrase(key: ByteArray): ByteArray {
        val hex = key.joinToString(separator = "") { "%02x".format(it) }
        return "x'$hex'".toByteArray(Charsets.US_ASCII)
    }
}
