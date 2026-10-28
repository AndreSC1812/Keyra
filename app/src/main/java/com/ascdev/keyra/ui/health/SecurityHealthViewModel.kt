package com.ascdev.keyra.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.domain.audit.AuditReport
import com.ascdev.keyra.domain.audit.PasswordAuditor
import com.ascdev.keyra.domain.repository.EntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SecurityHealthUiState(
    val isLoading: Boolean = true,
    val total: Int = 0,
    val report: AuditReport = AuditReport.Empty,
) {
    val healthy: Int get() = (total - report.issueCount).coerceAtLeast(0)
}

@HiltViewModel
class SecurityHealthViewModel @Inject constructor(
    repository: EntryRepository,
    auditor: PasswordAuditor,
) : ViewModel() {

    val state: StateFlow<SecurityHealthUiState> = repository.observeEntries()
        .map { entries -> SecurityHealthUiState(isLoading = false, total = entries.size, report = auditor.audit(entries)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SecurityHealthUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
