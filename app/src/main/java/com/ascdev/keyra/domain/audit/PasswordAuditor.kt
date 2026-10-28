package com.ascdev.keyra.domain.audit

import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import javax.inject.Inject

data class AuditReport(
    val weak: List<VaultEntry>,
    val reusedGroups: List<List<VaultEntry>>,
) {
    val weakIds: Set<Long> = weak.mapTo(HashSet()) { it.id }
    val reusedIds: Set<Long> = reusedGroups.flatten().mapTo(HashSet()) { it.id }
    val issueCount: Int get() = (weakIds + reusedIds).size

    fun hasIssues(id: Long): Boolean = id in weakIds || id in reusedIds

    companion object {
        val Empty = AuditReport(emptyList(), emptyList())
    }
}

/** Finds weak passwords and passwords shared between several entries. */
class PasswordAuditor @Inject constructor(
    private val strengthCalculator: PasswordStrengthCalculator,
) {

    fun audit(entries: List<VaultEntry>): AuditReport {
        val withPassword = entries.filter { it.password.isNotEmpty() }
        val weak = withPassword.filter { strengthCalculator.evaluate(it.password).level.isWeak }
        val reused = withPassword
            .groupBy { it.password }
            .values
            .filter { it.size > 1 }
        return AuditReport(weak = weak, reusedGroups = reused)
    }
}
