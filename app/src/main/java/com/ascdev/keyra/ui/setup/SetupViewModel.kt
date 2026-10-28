package com.ascdev.keyra.ui.setup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.R
import com.ascdev.keyra.core.security.VaultKeyManager
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.data.local.VaultDatabaseFactory
import com.ascdev.keyra.domain.pin.PinError
import com.ascdev.keyra.domain.pin.PinValidator
import com.ascdev.keyra.domain.pin.copyChars
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.inject.Inject

enum class SetupStep { WELCOME, CREATE_PIN, CONFIRM_PIN, BIOMETRIC }

data class SetupUiState(
    val step: SetupStep = SetupStep.WELCOME,
    val pinLength: Int = 0,
    @param:StringRes val error: Int? = null,
    val isWorking: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val vaultKeyManager: VaultKeyManager,
    private val session: VaultSession,
    private val databaseFactory: VaultDatabaseFactory,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    // Digits live only in memory and are wiped as soon as the vault is created.
    private val pin = StringBuilder()
    private var firstPin: CharArray? = null

    fun start() = _state.update { it.copy(step = SetupStep.CREATE_PIN) }

    fun onDigit(digit: Char) {
        if (pin.length >= PinValidator.MAX_LENGTH || _state.value.isWorking) return
        pin.append(digit)
        _state.update { it.copy(pinLength = pin.length, error = null) }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) pin.deleteCharAt(pin.lastIndex)
        _state.update { it.copy(pinLength = pin.length, error = null) }
    }

    fun onConfirm() {
        when (_state.value.step) {
            SetupStep.CREATE_PIN -> confirmFirstPin()
            SetupStep.CONFIRM_PIN -> confirmSecondPin()
            else -> Unit
        }
    }

    fun backToCreate() {
        firstPin?.fill(' ')
        firstPin = null
        clearPin()
        _state.update { it.copy(step = SetupStep.CREATE_PIN, error = null) }
    }

    private fun confirmFirstPin() {
        val error = PinValidator.validate(pin)
        if (error != null) {
            _state.update { it.copy(error = error.message()) }
            return
        }
        firstPin = pin.copyChars()
        clearPin()
        _state.update { it.copy(step = SetupStep.CONFIRM_PIN, error = null) }
    }

    private fun confirmSecondPin() {
        val expected = firstPin ?: return backToCreate()
        val entered = pin.copyChars()
        if (!entered.contentEquals(expected)) {
            entered.fill(' ')
            clearPin()
            _state.update { it.copy(error = R.string.setup_error_pin_mismatch) }
            return
        }
        clearPin()
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            databaseFactory.delete()
            val vaultKey = vaultKeyManager.createVault(entered)
            entered.fill(' ')
            expected.fill(' ')
            firstPin = null
            session.open(vaultKey)
            _state.update { it.copy(step = SetupStep.BIOMETRIC, isWorking = false) }
        }
    }

    fun biometricCipher(): Cipher = vaultKeyManager.biometricEncryptCipher()

    fun onBiometricAuthenticated(cipher: Cipher) {
        viewModelScope.launch {
            runCatching { session.withVaultKey { key -> key.copyOf() } }
                .onSuccess { key ->
                    vaultKeyManager.enableBiometric(cipher, key)
                    key.fill(0)
                }
            finish()
        }
    }

    fun onBiometricError(@StringRes message: Int) = _state.update { it.copy(error = message) }

    fun skipBiometric() {
        viewModelScope.launch { finish() }
    }

    private suspend fun finish() = vaultKeyManager.completeSetup()

    private fun clearPin() {
        pin.setLength(0)
        _state.update { it.copy(pinLength = 0) }
    }

    private fun PinError.message(): Int = when (this) {
        PinError.TOO_SHORT -> R.string.setup_error_pin_short
        PinError.TOO_LONG -> R.string.setup_error_pin_long
        PinError.NOT_NUMERIC -> R.string.setup_error_pin_short
        PinError.TOO_SIMPLE -> R.string.setup_error_pin_simple
    }

    override fun onCleared() {
        pin.setLength(0)
        firstPin?.fill(' ')
    }
}
