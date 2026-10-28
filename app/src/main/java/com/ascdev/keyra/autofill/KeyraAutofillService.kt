package com.ascdev.keyra.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.ascdev.keyra.core.di.ApplicationScope
import com.ascdev.keyra.core.security.VaultKeyManager
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.domain.autofill.EntryMatcher
import com.ascdev.keyra.domain.repository.EntryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Offers matching logins to other apps and websites. When the vault is locked it only offers
 * an "Unlock Keyra" item, which opens [AutofillAuthActivity]; credentials never leave the
 * vault until the user authenticates.
 */
@AndroidEntryPoint
class KeyraAutofillService : AutofillService() {

    @Inject lateinit var session: VaultSession

    @Inject lateinit var repository: EntryRepository

    @Inject lateinit var vaultKeyManager: VaultKeyManager

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: return callback.onSuccess(null)
        val fields = AutofillParser.parse(structure)
        if (fields.isEmpty || fields.packageName == packageName) return callback.onSuccess(null)

        val job = scope.launch {
            if (!vaultKeyManager.isSetupComplete.first()) return@launch callback.onSuccess(null)
            val matches = if (session.isUnlocked.value) {
                runCatching { EntryMatcher.filter(repository.getEntries(), fields.packageName, fields.webDomain) }
                    .getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val response = FillResponse.Builder().apply {
                matches.take(MAX_DATASETS).forEach { addDataset(DatasetFactory.forEntry(this@KeyraAutofillService, it, fields)) }
                addDataset(DatasetFactory.unlockDataset(this@KeyraAutofillService, fields, request.id))
            }.build()
            callback.onSuccess(response)
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Saving from other apps is not supported yet; entries are created inside Keyra.
        callback.onSuccess()
    }

    private companion object {
        const val MAX_DATASETS = 5
    }
}
