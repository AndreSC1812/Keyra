package com.ascdev.keyra.domain.autofill

import com.ascdev.keyra.domain.model.VaultEntry

/**
 * Matches vault entries against the app or website that asked for autofill.
 * Websites are matched by domain, apps by the most meaningful segment of the package name
 * (com.instagram.android -> "instagram").
 */
object EntryMatcher {

    private val IGNORED_SEGMENTS = setOf(
        "com", "org", "net", "io", "app", "apps", "android", "mobile", "www", "m", "client", "co",
    )

    fun matchKey(packageName: String?, webDomain: String?): String? {
        if (!webDomain.isNullOrBlank()) {
            val labels = webDomain.lowercase().removePrefix("www.").removePrefix("m.").split('.')
            return labels.dropLast(1).lastOrNull { it.isNotBlank() } ?: labels.firstOrNull()
        }
        if (packageName.isNullOrBlank()) return null
        return packageName.lowercase()
            .split('.')
            .filter { it.length > 1 && it !in IGNORED_SEGMENTS }
            .maxByOrNull { it.length }
    }

    fun matches(entry: VaultEntry, key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        return entry.url.contains(key, ignoreCase = true) ||
            entry.title.contains(key, ignoreCase = true)
    }

    fun filter(entries: List<VaultEntry>, packageName: String?, webDomain: String?): List<VaultEntry> {
        val key = matchKey(packageName, webDomain)
        return entries.filter { matches(it, key) }
    }
}
