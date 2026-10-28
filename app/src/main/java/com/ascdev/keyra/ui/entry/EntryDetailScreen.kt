package com.ascdev.keyra.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.R
import com.ascdev.keyra.ui.components.CategoryBadge
import com.ascdev.keyra.ui.components.StrengthMeter
import com.ascdev.keyra.ui.components.VisibilityToggle
import com.ascdev.keyra.ui.components.icon
import com.ascdev.keyra.ui.components.label
import com.ascdev.keyra.ui.components.maskSecret
import com.ascdev.keyra.ui.theme.SecretTextStyle
import com.ascdev.keyra.ui.theme.warning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.copied_generic)

    LaunchedEffect(state.isLoading, state.entry) {
        if (!state.isLoading && state.entry == null) onBack()
    }

    val copy: (String, String, Boolean) -> Unit = { label, value, sensitive ->
        viewModel.copy(label, value, sensitive)
        if (viewModel.showsCopyConfirmation) scope.launch { snackbar.showSnackbar(copied) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit)) }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val entry = state.entry ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(entry.category, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(entry.category.label)) },
                        leadingIcon = { Icon(entry.category.icon, contentDescription = null) },
                    )
                }
            }

            if (state.isWeak || state.reusedCount > 1) {
                WarningBanner(isWeak = state.isWeak, reusedCount = state.reusedCount)
            }

            val usernameLabel = stringResource(R.string.field_username)
            val passwordLabel = stringResource(R.string.field_password)
            if (entry.username.isNotBlank()) {
                DetailField(usernameLabel, entry.username, onCopy = { copy(usernameLabel, entry.username, false) })
            }
            DetailField(
                label = passwordLabel,
                value = if (state.passwordVisible) entry.password else maskSecret(entry.password),
                valueStyle = SecretTextStyle,
                onCopy = { copy(passwordLabel, entry.password, true) },
                extra = { VisibilityToggle(state.passwordVisible, viewModel::togglePasswordVisibility) },
                footer = { state.strength?.let { StrengthMeter(it, Modifier.padding(top = 12.dp)) } },
            )
            if (entry.url.isNotBlank()) {
                val urlLabel = stringResource(R.string.field_url)
                DetailField(urlLabel, entry.url, onCopy = { copy(urlLabel, entry.url, false) })
            }
            if (entry.notes.isNotBlank()) {
                DetailField(stringResource(R.string.field_notes), entry.notes)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_entry_title)) },
            text = { Text(stringResource(R.string.delete_entry_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun WarningBanner(isWeak: Boolean, reusedCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.warning)
            Spacer(Modifier.width(12.dp))
            Column {
                if (isWeak) {
                    Text(
                        stringResource(R.string.warning_weak_password),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (reusedCount > 1) {
                    Text(
                        pluralStringResource(R.plurals.warning_reused_password, reusedCount - 1, reusedCount - 1),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onCopy: (() -> Unit)? = null,
    extra: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(value, style = valueStyle)
                }
                extra()
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            stringResource(R.string.action_copy_field, label),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(Modifier.padding(end = 12.dp)) { footer() }
        }
    }
}
