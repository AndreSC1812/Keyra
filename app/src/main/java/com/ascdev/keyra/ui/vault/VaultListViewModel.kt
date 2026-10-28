package com.ascdev.keyra.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.core.clipboard.SecureClipboard
import com.ascdev.keyra.core.session.AutoLockManager
import com.ascdev.keyra.domain.audit.AuditReport
import com.ascdev.keyra.domain.audit.PasswordAuditor
import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.repository.EntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VaultListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val category: Category? = null,
    val entries: List<VaultEntry> = emptyList(),
    val totalCount: Int = 0,
    val audit: AuditReport = AuditReport.Empty,
)

@HiltViewModel
class VaultListViewModel @Inject constructor(
    repository: EntryRepository,
    private val auditor: PasswordAuditor,
    private val clipboard: SecureClipboard,
    private val autoLockManager: AutoLockManager,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<Category?>(null)

    val state: StateFlow<VaultListUiState> =
        combine(repository.observeEntries(), query, category) { entries, query, category ->
            VaultListUiState(
                isLoading = false,
                query = query,
                category = category,
                entries = filterEntries(entries, query, category),
                totalCount = entries.size,
                audit = auditor.audit(entries),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), VaultListUiState())

    val showsCopyConfirmation: Boolean get() = !clipboard.showsSystemConfirmation

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: Category?) {
        category.value = value
    }

    fun copyPassword(entry: VaultEntry) = clipboard.copy(entry.title, entry.password)

    fun lock() = autoLockManager.lockNow()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Matches the query against title, username and URL, then applies the category filter. */
fun filterEntries(entries: List<VaultEntry>, query: String, category: Category?): List<VaultEntry> {
    val trimmed = query.trim()
    return entries.filter { entry ->
        (category == null || entry.category == category) &&
            (
                trimmed.isEmpty() ||
                    entry.title.contains(trimmed, ignoreCase = true) ||
                    entry.username.contains(trimmed, ignoreCase = true) ||
                    entry.url.contains(trimmed, ignoreCase = true)
                )
    }
}
