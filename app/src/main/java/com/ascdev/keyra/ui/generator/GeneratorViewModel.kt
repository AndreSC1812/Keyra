package com.ascdev.keyra.ui.generator

import androidx.lifecycle.ViewModel
import com.ascdev.keyra.core.clipboard.SecureClipboard
import com.ascdev.keyra.domain.generator.GeneratorOptions
import com.ascdev.keyra.domain.generator.PasswordGenerator
import com.ascdev.keyra.domain.strength.PasswordStrength
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GeneratorUiState(
    val options: GeneratorOptions,
    val password: String,
    val strength: PasswordStrength,
) {
    override fun toString(): String = "GeneratorUiState(options=$options)"
}

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generator: PasswordGenerator,
    private val strengthCalculator: PasswordStrengthCalculator,
    private val clipboard: SecureClipboard,
) : ViewModel() {

    private val _state = MutableStateFlow(build(GeneratorOptions()))
    val state: StateFlow<GeneratorUiState> = _state.asStateFlow()

    val showsCopyConfirmation: Boolean get() = !clipboard.showsSystemConfirmation

    fun regenerate() {
        _state.value = build(_state.value.options)
    }

    fun onLengthChange(length: Int) = updateOptions { copy(length = length) }
    fun onUppercaseChange(enabled: Boolean) = updateOptions { copy(uppercase = enabled) }
    fun onLowercaseChange(enabled: Boolean) = updateOptions { copy(lowercase = enabled) }
    fun onDigitsChange(enabled: Boolean) = updateOptions { copy(digits = enabled) }
    fun onSymbolsChange(enabled: Boolean) = updateOptions { copy(symbols = enabled) }

    fun copy(label: String) = clipboard.copy(label, _state.value.password)

    private fun updateOptions(block: GeneratorOptions.() -> GeneratorOptions) {
        val options = _state.value.options.block()
        // At least one character set must stay selected.
        if (!options.uppercase && !options.lowercase && !options.digits && !options.symbols) return
        if (options == _state.value.options) return
        _state.value = build(options)
    }

    private fun build(options: GeneratorOptions): GeneratorUiState {
        val password = generator.generate(options)
        return GeneratorUiState(options, password, strengthCalculator.evaluate(password))
    }
}
