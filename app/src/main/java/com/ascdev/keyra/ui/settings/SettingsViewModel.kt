package com.ascdev.keyra.ui.settings

import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.R
import com.ascdev.keyra.core.security.VaultKeyManager
import com.ascdev.keyra.core.session.AutoLockManager
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.core.settings.AppSettings
import com.ascdev.keyra.core.settings.SettingsRepository
import com.ascdev.keyra.core.settings.ThemeMode
import com.ascdev.keyra.data.backup.BackupManager
import com.ascdev.keyra.data.backup.InvalidBackupException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.inject.Inject

sealed interface SettingsMessage {
    data class Text(@param:StringRes val text: Int) : SettingsMessage
    data class Count(@param:PluralsRes val text: Int, val count: Int) : SettingsMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val vaultKeyManager: VaultKeyManager,
    private val session: VaultSession,
    private val autoLockManager: AutoLockManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning: StateFlow<Boolean> = _isBackupRunning.asStateFlow()

    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages: Flow<SettingsMessage> = _messages.receiveAsFlow()

    // Kept only while the file picker is open.
    private var pendingExportPassword: CharArray? = null

    fun setAutoLockMinutes(minutes: Int) = launch { settingsRepository.setAutoLockMinutes(minutes) }

    fun setLockOnBackground(enabled: Boolean) = launch { settingsRepository.setLockOnBackground(enabled) }

    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }

    fun biometricCipher(): Cipher? = runCatching { vaultKeyManager.biometricEncryptCipher() }.getOrNull()

    fun onBiometricAuthenticated(cipher: Cipher) = launch {
        try {
            val key = session.withVaultKey { it.copyOf() }
            vaultKeyManager.enableBiometric(cipher, key)
            key.fill(0)
            _messages.send(SettingsMessage.Text(R.string.settings_biometric_enabled))
        } catch (_: GeneralSecurityException) {
            _messages.send(SettingsMessage.Text(R.string.biometric_error_generic))
        }
    }

    fun disableBiometric() = launch { vaultKeyManager.disableBiometric() }

    fun onBiometricError() = launch { _messages.send(SettingsMessage.Text(R.string.biometric_error_generic)) }

    /** Stops the vault from locking while the system file picker is in front. */
    fun beforeFilePicker() = autoLockManager.pauseBackgroundLock()

    fun prepareExport(password: CharArray) {
        pendingExportPassword?.fill(' ')
        pendingExportPassword = password
        beforeFilePicker()
    }

    fun export(uri: Uri?) {
        autoLockManager.resumeBackgroundLock()
        val password = pendingExportPassword ?: return
        pendingExportPassword = null
        if (uri == null) {
            password.fill(' ')
            return
        }
        runBackup(onSuccess = R.plurals.settings_export_success, password = password) {
            backupManager.export(uri, password)
        }
    }

    fun onImportFilePicked() = autoLockManager.resumeBackgroundLock()

    fun import(uri: Uri, password: CharArray) =
        runBackup(onSuccess = R.plurals.settings_import_success, password = password) {
            backupManager.import(uri, password)
        }

    fun lockNow() = autoLockManager.lockNow()

    private fun runBackup(@PluralsRes onSuccess: Int, password: CharArray, block: suspend () -> Int) = launch {
        _isBackupRunning.value = true
        val message = try {
            SettingsMessage.Count(onSuccess, block())
        } catch (_: InvalidBackupException) {
            SettingsMessage.Text(R.string.settings_backup_invalid)
        } catch (_: IOException) {
            SettingsMessage.Text(R.string.settings_backup_io_error)
        } finally {
            password.fill(' ')
            _isBackupRunning.value = false
        }
        _messages.send(message)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        pendingExportPassword?.fill(' ')
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
