package com.ascdev.keyra.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ascdev.keyra.core.clipboard.SecureClipboard
import com.ascdev.keyra.domain.audit.PasswordAuditor
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import com.ascdev.keyra.domain.strength.PasswordStrength
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import com.ascdev.keyra.ui.navigation.EntryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryDetailUiState(
    val isLoading: Boolean = true,
    val entry: VaultEntry? = null,
    val strength: PasswordStrength? = null,
    val isWeak: Boolean = false,
    val reusedCount: Int = 0,
    val passwordVisible: Boolean = false,
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EntryRepository,
    private val strengthCalculator: PasswordStrengthCalculator,
    private val auditor: PasswordAuditor,
    private val clipboard: SecureClipboard,
) : ViewModel() {

    private val id = savedStateHandle.toRoute<EntryDetailRoute>().id
    private val passwordVisible = MutableStateFlow(false)

    val state: StateFlow<EntryDetailUiState> =
        combine(repository.observeEntries(), passwordVisible) { entries, visible ->
            val entry = entries.firstOrNull { it.id == id }
            val report = auditor.audit(entries)
            EntryDetailUiState(
                isLoading = false,
                entry = entry,
                strength = entry?.let { strengthCalculator.evaluate(it.password) },
                isWeak = entry != null && entry.id in report.weakIds,
                reusedCount = report.reusedGroups.firstOrNull { group -> group.any { it.id == id } }?.size ?: 0,
                passwordVisible = visible,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EntryDetailUiState())

    val showsCopyConfirmation: Boolean get() = !clipboard.showsSystemConfirmation

    fun togglePasswordVisibility() {
        passwordVisible.value = !passwordVisible.value
    }

    fun copy(label: String, value: String, sensitive: Boolean) = clipboard.copy(label, value, sensitive)

    /** The screen closes itself once the entry disappears from the stream. */
    fun delete() {
        viewModelScope.launch { repository.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
