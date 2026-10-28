package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.autofill.EntryMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryMatcherTest {

    @Test
    fun `extracts the key from web domains`() {
        assertEquals("github", EntryMatcher.matchKey(null, "www.github.com"))
        assertEquals("bbc", EntryMatcher.matchKey(null, "m.bbc.co"))
        assertEquals("accounts", EntryMatcher.matchKey(null, "accounts.google"))
    }

    @Test
    fun `extracts the key from package names`() {
        assertEquals("instagram", EntryMatcher.matchKey("com.instagram.android", null))
        assertEquals("spotify", EntryMatcher.matchKey("com.spotify.music", null))
        assertNull(EntryMatcher.matchKey(null, null))
    }

    @Test
    fun `web domain wins over the browser package`() {
        assertEquals("netflix", EntryMatcher.matchKey("com.android.chrome", "www.netflix.com"))
    }

    @Test
    fun `filters entries by url or title`() {
        val entries = listOf(
            entry(1, title = "GitHub", url = "https://github.com/login"),
            entry(2, title = "Instagram"),
            entry(3, title = "Bank", url = "https://mybank.com"),
        )
        assertEquals(listOf(1L), EntryMatcher.filter(entries, null, "github.com").map { it.id })
        assertEquals(listOf(2L), EntryMatcher.filter(entries, "com.instagram.android", null).map { it.id })
        assertEquals(emptyList<Long>(), EntryMatcher.filter(entries, "com.unknown.app", null).map { it.id })
    }
}
