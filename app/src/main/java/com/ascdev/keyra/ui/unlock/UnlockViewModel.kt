package com.ascdev.keyra.ui.unlock

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.R
import com.ascdev.keyra.core.security.PinUnlockResult
import com.ascdev.keyra.core.security.VaultKeyManager
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.core.settings.SettingsRepository
import com.ascdev.keyra.domain.pin.PinValidator
import com.ascdev.keyra.domain.pin.copyChars
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.inject.Inject

data class UnlockUiState(
    val pinLength: Int = 0,
    val biometricEnabled: Boolean = false,
    val isWorking: Boolean = false,
    val lockoutSeconds: Int = 0,
    @param:StringRes val error: Int? = null,
    val attemptsLeft: Int? = null,
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val vaultKeyManager: VaultKeyManager,
    private val session: VaultSession,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    private val pin = StringBuilder()
    private var countdown: Job? = null

    /**
     * Called every time the lock screen is shown. The ViewModel lives as long as the activity,
     * so it has to start clean after each lock.
     */
    fun onShown() {
        pin.setLength(0)
        _state.value = UnlockUiState()
        viewModelScope.launch {
            _state.update { it.copy(biometricEnabled = settings.current().biometricEnabled) }
            startCountdown(vaultKeyManager.currentLockoutUntil())
        }
    }

    fun onDigit(digit: Char) {
        if (pin.length >= PinValidator.MAX_LENGTH || isBlocked()) return
        pin.append(digit)
        _state.update { it.copy(pinLength = pin.length, error = null) }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) pin.deleteCharAt(pin.lastIndex)
        _state.update { it.copy(pinLength = pin.length) }
    }

    fun onConfirm() {
        if (pin.isEmpty() || isBlocked()) return
        val entered = pin.copyChars()
        pin.setLength(0)
        _state.update { it.copy(isWorking = true, pinLength = 0) }
        viewModelScope.launch {
            val result = vaultKeyManager.unlockWithPin(entered)
            entered.fill(' ')
            when (result) {
                is PinUnlockResult.Success -> {
                    session.open(result.vaultKey)
                    _state.update { UnlockUiState() }
                }
                is PinUnlockResult.WrongPin -> {
                    _state.update {
                        it.copy(isWorking = false, error = R.string.unlock_error_wrong_pin, attemptsLeft = result.attemptsLeft)
                    }
                    startCountdown(result.lockoutUntil)
                }
                is PinUnlockResult.LockedOut -> {
                    _state.update { it.copy(isWorking = false) }
                    startCountdown(result.until)
                }
            }
        }
    }

    /** Returns the cipher for BiometricPrompt, or null if biometrics stopped being valid. */
    suspend fun biometricCipher(): Cipher? {
        val cipher = vaultKeyManager.biometricDecryptCipher()
        if (cipher == null) {
            _state.update { it.copy(biometricEnabled = false, error = R.string.unlock_error_biometric_changed) }
        }
        return cipher
    }

    fun onBiometricAuthenticated(cipher: Cipher) {
        viewModelScope.launch {
            try {
                session.open(vaultKeyManager.unlockWithBiometric(cipher))
            } catch (_: GeneralSecurityException) {
                _state.update { it.copy(error = R.string.biometric_error_generic) }
            }
        }
    }

    fun onBiometricError() = _state.update { it.copy(error = R.string.biometric_error_generic) }

    private fun isBlocked() = _state.value.isWorking || _state.value.lockoutSeconds > 0

    private fun startCountdown(until: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (true) {
                val remaining = ((until - System.currentTimeMillis()) / SECOND_MS).toInt()
                if (remaining <= 0) break
                _state.update { it.copy(lockoutSeconds = remaining) }
                delay(SECOND_MS)
            }
            _state.update { it.copy(lockoutSeconds = 0) }
        }
    }

    override fun onCleared() {
        pin.setLength(0)
    }

    private companion object {
        const val SECOND_MS = 1_000L
    }
}
