package com.ascdev.keyra.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import com.ascdev.keyra.R
import com.ascdev.keyra.core.security.applySecureWindow
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.core.settings.AppSettings
import com.ascdev.keyra.core.settings.SettingsRepository
import com.ascdev.keyra.domain.autofill.EntryMatcher
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import com.ascdev.keyra.ui.components.CategoryBadge
import com.ascdev.keyra.ui.theme.KeyraTheme
import com.ascdev.keyra.ui.theme.SyncSystemBars
import com.ascdev.keyra.ui.theme.isKeyraDark
import com.ascdev.keyra.ui.unlock.UnlockScreen
import com.ascdev.keyra.ui.vault.filterEntries
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Unlocks the vault on behalf of another app and returns the login the user picks. */
@AndroidEntryPoint
class AutofillAuthActivity : FragmentActivity() {

    @Inject lateinit var session: VaultSession

    @Inject lateinit var repository: EntryRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        applySecureWindow()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        val fields = ParsedFields(
            usernameIds = intent.autofillIds(EXTRA_USERNAME_IDS),
            passwordIds = intent.autofillIds(EXTRA_PASSWORD_IDS),
            packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME),
            webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN),
        )

        setContent {
            val settings by settingsRepository.settings.collectAsState(AppSettings())
            val unlocked by session.isUnlocked.collectAsState()
            SyncSystemBars(isKeyraDark(settings.themeMode))
            KeyraTheme(settings.themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (unlocked) {
                        val entries by repository.observeEntries().collectAsState(emptyList())
                        EntryPicker(entries, fields) { entry -> finishWith(entry, fields) }
                    } else {
                        UnlockScreen()
                    }
                }
            }
        }
    }

    private fun finishWith(entry: VaultEntry, fields: ParsedFields) {
        val dataset = DatasetFactory.forEntry(this, entry, fields)
        setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset))
        finish()
    }

    private fun Intent.autofillIds(key: String): List<AutofillId> =
        IntentCompat.getParcelableArrayListExtra(this, key, AutofillId::class.java).orEmpty()

    companion object {
        const val EXTRA_USERNAME_IDS = "com.ascdev.keyra.extra.USERNAME_IDS"
        const val EXTRA_PASSWORD_IDS = "com.ascdev.keyra.extra.PASSWORD_IDS"
        const val EXTRA_PACKAGE_NAME = "com.ascdev.keyra.extra.PACKAGE_NAME"
        const val EXTRA_WEB_DOMAIN = "com.ascdev.keyra.extra.WEB_DOMAIN"
    }
}

@Composable
private fun EntryPicker(entries: List<VaultEntry>, fields: ParsedFields, onPick: (VaultEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    val suggested = remember(entries, fields) { EntryMatcher.filter(entries, fields.packageName, fields.webDomain) }
    val results = remember(entries, query) { filterEntries(entries, query, null) }
    val source = fields.webDomain ?: fields.packageName.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.autofill_pick_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.autofill_pick_subtitle, source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.vault_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (query.isBlank() && suggested.isNotEmpty()) {
                item { PickerHeader(stringResource(R.string.autofill_suggested)) }
                items(suggested, key = { "s-${it.id}" }) { PickerRow(it, onPick) }
                item { PickerHeader(stringResource(R.string.autofill_all)) }
            }
            items(results, key = { it.id }) { PickerRow(it, onPick) }
        }
    }
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PickerRow(entry: VaultEntry, onPick: (VaultEntry) -> Unit) {
    Surface(
        onClick = { onPick(entry) },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(entry.category, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                Text(entry.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
