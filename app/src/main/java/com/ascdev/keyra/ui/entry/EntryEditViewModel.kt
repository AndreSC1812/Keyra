package com.ascdev.keyra.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import com.ascdev.keyra.domain.strength.PasswordStrength
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import com.ascdev.keyra.ui.navigation.EntryEditRoute
import com.ascdev.keyra.ui.navigation.NEW_ENTRY_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryForm(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val category: Category = Category.LOGIN,
) {
    val isValid: Boolean get() = title.isNotBlank() && password.isNotEmpty()

    override fun toString(): String = "EntryForm(title=$title)"
}

data class EntryEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val form: EntryForm = EntryForm(),
    val strength: PasswordStrength? = null,
    val passwordVisible: Boolean = false,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
)

/**
 * The form is kept only in memory on purpose: SavedStateHandle would persist the password
 * in the saved instance state bundle.
 */
@HiltViewModel
class EntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val strengthCalculator: PasswordStrengthCalculator,
) : ViewModel() {

    private val id = savedStateHandle.toRoute<EntryEditRoute>().id
    private var original: VaultEntry? = null

    private val _state = MutableStateFlow(EntryEditUiState(isNew = id == NEW_ENTRY_ID, isLoading = id != NEW_ENTRY_ID))
    val state: StateFlow<EntryEditUiState> = _state.asStateFlow()

    init {
        if (id != NEW_ENTRY_ID) {
            viewModelScope.launch {
                val entry = repository.getEntry(id)
                original = entry
                val form = entry?.let {
                    EntryForm(it.title, it.username, it.password, it.url, it.notes, it.category)
                } ?: EntryForm()
                _state.update { it.copy(isLoading = false, form = form, strength = evaluate(form.password)) }
            }
        }
    }

    fun onTitleChange(value: String) = updateForm { copy(title = value) }
    fun onUsernameChange(value: String) = updateForm { copy(username = value) }
    fun onUrlChange(value: String) = updateForm { copy(url = value) }
    fun onNotesChange(value: String) = updateForm { copy(notes = value) }
    fun onCategoryChange(value: Category) = updateForm { copy(category = value) }

    fun onPasswordChange(value: String) {
        updateForm { copy(password = value) }
        _state.update { it.copy(strength = evaluate(value)) }
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun save(onSaved: () -> Unit) {
        val form = _state.value.form
        if (!form.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.save(
                VaultEntry(
                    id = original?.id ?: 0,
                    title = form.title.trim(),
                    username = form.username.trim(),
                    password = form.password,
                    url = form.url.trim(),
                    notes = form.notes.trim(),
                    category = form.category,
                    createdAt = original?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            onSaved()
        }
    }

    private fun evaluate(password: String) =
        if (password.isEmpty()) null else strengthCalculator.evaluate(password)

    private inline fun updateForm(crossinline block: EntryForm.() -> EntryForm) =
        _state.update { it.copy(form = it.form.block()) }
}
