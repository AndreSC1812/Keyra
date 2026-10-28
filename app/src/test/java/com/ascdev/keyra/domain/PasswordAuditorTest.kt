package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.audit.PasswordAuditor
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordAuditorTest {

    private val auditor = PasswordAuditor(PasswordStrengthCalculator())

    @Test
    fun `detects weak passwords`() {
        val report = auditor.audit(listOf(entry(1, password = "123456"), entry(2)))
        assertEquals(setOf(1L), report.weakIds)
        assertTrue(report.hasIssues(1))
        assertFalse(report.hasIssues(2))
    }

    @Test
    fun `groups reused passwords`() {
        val shared = "Shared#Pass-2026!"
        val report = auditor.audit(
            listOf(entry(1, password = shared), entry(2, password = shared), entry(3), entry(4, password = shared)),
        )
        assertEquals(1, report.reusedGroups.size)
        assertEquals(setOf(1L, 2L, 4L), report.reusedIds)
        assertEquals(3, report.issueCount)
    }

    @Test
    fun `counts an entry that is weak and reused only once`() {
        val report = auditor.audit(listOf(entry(1, password = "123456"), entry(2, password = "123456")))
        assertEquals(2, report.issueCount)
    }

    @Test
    fun `a healthy vault has no issues`() {
        val report = auditor.audit(listOf(entry(1), entry(2), entry(3)))
        assertEquals(0, report.issueCount)
    }
}
