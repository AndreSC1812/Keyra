package com.ascdev.keyra.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.BuildConfig
import com.ascdev.keyra.R
import com.ascdev.keyra.core.settings.AppSettings
import com.ascdev.keyra.core.settings.ThemeMode
import com.ascdev.keyra.ui.components.BiometricAuthenticator
import com.ascdev.keyra.ui.components.SecretKeyboardOptions
import com.ascdev.keyra.ui.components.secretTransformation
import java.time.LocalDate

private enum class SettingsDialog { AUTO_LOCK, THEME, EXPORT_PASSWORD, IMPORT_PASSWORD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isBackupRunning by viewModel.isBackupRunning.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as FragmentActivity
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var autofillEnabled by remember { mutableStateOf(false) }
    val biometricAvailable = remember { BiometricAuthenticator.isAvailable(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        autofillEnabled = context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices() == true
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is SettingsMessage.Count -> resources.getQuantityString(message.text, message.count, message.count)
                is SettingsMessage.Text -> resources.getString(message.text)
            }
            snackbar.showSnackbar(text)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
        viewModel::export,
    )
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onImportFilePicked()
        if (uri != null) {
            importUri = uri
            dialog = SettingsDialog.IMPORT_PASSWORD
        }
    }

    val biometricTitle = stringResource(R.string.biometric_enable_title)
    val biometricSubtitle = stringResource(R.string.biometric_enable_subtitle)
    val cancel = stringResource(R.string.action_cancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (isBackupRunning) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionHeader(R.string.settings_section_security)
            SwitchItem(
                icon = Icons.Outlined.Fingerprint,
                title = stringResource(R.string.settings_biometric),
                subtitle = stringResource(
                    if (biometricAvailable) R.string.settings_biometric_subtitle else R.string.setup_biometric_unavailable,
                ),
                checked = settings.biometricEnabled,
                enabled = biometricAvailable,
                onCheckedChange = { enable ->
                    if (!enable) {
                        viewModel.disableBiometric()
                        return@SwitchItem
                    }
                    val cipher = viewModel.biometricCipher() ?: return@SwitchItem viewModel.onBiometricError()
                    BiometricAuthenticator.authenticate(
                        activity = activity,
                        cipher = cipher,
                        title = biometricTitle,
                        subtitle = biometricSubtitle,
                        negativeText = cancel,
                        onSuccess = viewModel::onBiometricAuthenticated,
                        onError = { message -> if (message != null) viewModel.onBiometricError() },
                    )
                },
            )
            ClickItem(
                icon = Icons.Outlined.Timer,
                title = stringResource(R.string.settings_auto_lock),
                subtitle = pluralStringResource(R.plurals.minutes, settings.autoLockMinutes, settings.autoLockMinutes),
                onClick = { dialog = SettingsDialog.AUTO_LOCK },
            )
            SwitchItem(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(R.string.settings_lock_on_background),
                subtitle = stringResource(R.string.settings_lock_on_background_subtitle),
                checked = settings.lockOnBackground,
                onCheckedChange = viewModel::setLockOnBackground,
            )
            ClickItem(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.settings_lock_now),
                subtitle = stringResource(R.string.settings_lock_now_subtitle),
                onClick = viewModel::lockNow,
            )

            SectionHeader(R.string.settings_section_autofill)
            ClickItem(
                icon = Icons.Outlined.Password,
                title = stringResource(R.string.settings_autofill),
                subtitle = stringResource(
                    if (autofillEnabled) R.string.settings_autofill_enabled else R.string.settings_autofill_disabled,
                ),
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                        .setData("package:${context.packageName}".toUri())
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
            )

            SectionHeader(R.string.settings_section_backup)
            ClickItem(
                icon = Icons.Outlined.Upload,
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_subtitle),
                enabled = !isBackupRunning,
                onClick = { dialog = SettingsDialog.EXPORT_PASSWORD },
            )
            ClickItem(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.settings_import),
                subtitle = stringResource(R.string.settings_import_subtitle),
                enabled = !isBackupRunning,
                onClick = {
                    viewModel.beforeFilePicker()
                    importLauncher.launch(arrayOf("*/*"))
                },
            )

            SectionHeader(R.string.settings_section_appearance)
            ClickItem(
                icon = Icons.Outlined.DarkMode,
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(settings.themeMode.label),
                onClick = { dialog = SettingsDialog.THEME },
            )

            SectionHeader(R.string.settings_section_about)
            ClickItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = null,
            )
        }
    }

    when (dialog) {
        SettingsDialog.AUTO_LOCK -> ChoiceDialog(
            title = stringResource(R.string.settings_auto_lock),
            options = AppSettings.AUTO_LOCK_OPTIONS,
            selected = settings.autoLockMinutes,
            label = { pluralStringResource(R.plurals.minutes, it, it) },
            onSelect = viewModel::setAutoLockMinutes,
            onDismiss = { dialog = null },
        )
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { stringResource(it.label) },
            onSelect = viewModel::setThemeMode,
            onDismiss = { dialog = null },
        )
        SettingsDialog.EXPORT_PASSWORD -> BackupPasswordDialog(
            title = stringResource(R.string.settings_export),
            message = stringResource(R.string.backup_password_export_message),
            requireConfirmation = true,
            onConfirm = { password ->
                dialog = null
                viewModel.prepareExport(password)
                exportLauncher.launch("keyra-backup-${LocalDate.now()}.keyra")
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.IMPORT_PASSWORD -> BackupPasswordDialog(
            title = stringResource(R.string.settings_import),
            message = stringResource(R.string.backup_password_import_message),
            requireConfirmation = false,
            onConfirm = { password ->
                dialog = null
                importUri?.let { viewModel.import(it, password) }
                importUri = null
            },
            onDismiss = {
                dialog = null
                importUri = null
            },
        )
        null -> Unit
    }
}

private const val BACKUP_MIME_TYPE = "application/octet-stream"
private const val MIN_BACKUP_PASSWORD = 8

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.SYSTEM -> R.string.theme_system
    }

@Composable
private fun SectionHeader(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun ClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier,
    )
}

@Composable
private fun SwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    message: String,
    requireConfirmation: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val tooShort = password.length < MIN_BACKUP_PASSWORD
    val mismatch = requireConfirmation && confirmation != password
    val canConfirm = if (requireConfirmation) !tooShort && !mismatch else password.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    singleLine = true,
                    visualTransformation = secretTransformation(visible),
                    keyboardOptions = SecretKeyboardOptions,
                    supportingText = if (requireConfirmation) {
                        { Text(pluralStringResource(R.plurals.backup_password_min, MIN_BACKUP_PASSWORD, MIN_BACKUP_PASSWORD)) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(stringResource(R.string.backup_password_confirm)) },
                        singleLine = true,
                        isError = confirmation.isNotEmpty() && mismatch,
                        visualTransformation = secretTransformation(visible),
                        keyboardOptions = SecretKeyboardOptions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { visible = !visible }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(checked = visible, onCheckedChange = null)
                    Text(stringResource(R.string.action_show_password), modifier = Modifier.padding(start = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.toCharArray()) }, enabled = canConfirm) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
