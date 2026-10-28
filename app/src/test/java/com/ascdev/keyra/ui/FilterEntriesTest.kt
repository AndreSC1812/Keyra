package com.ascdev.keyra.ui

import com.ascdev.keyra.domain.entry
import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.ui.vault.filterEntries
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterEntriesTest {

    private val entries = listOf(
        entry(1, title = "GitHub", username = "andre", url = "github.com", category = Category.WORK),
        entry(2, title = "Gmail", username = "andre@gmail.com", category = Category.EMAIL),
        entry(3, title = "Bank", username = "client-42", url = "mybank.com", category = Category.FINANCE),
    )

    @Test
    fun `empty query returns everything`() {
        assertEquals(3, filterEntries(entries, "  ", null).size)
    }

    @Test
    fun `query matches title, username and url ignoring case`() {
        assertEquals(listOf(1L), filterEntries(entries, "GITHUB", null).map { it.id })
        assertEquals(listOf(1L, 2L), filterEntries(entries, "andre", null).map { it.id })
        assertEquals(listOf(3L), filterEntries(entries, "mybank", null).map { it.id })
    }

    @Test
    fun `category and query are combined`() {
        assertEquals(listOf(2L), filterEntries(entries, "andre", Category.EMAIL).map { it.id })
        assertEquals(emptyList<Long>(), filterEntries(entries, "bank", Category.EMAIL).map { it.id })
    }
}
