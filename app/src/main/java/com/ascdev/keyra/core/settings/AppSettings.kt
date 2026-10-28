package com.ascdev.keyra.core.settings

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val autoLockMinutes: Int = DEFAULT_AUTO_LOCK_MINUTES,
    val lockOnBackground: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val biometricEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_AUTO_LOCK_MINUTES = 5
        val AUTO_LOCK_OPTIONS = listOf(1, 2, 5, 10, 15)
    }
}
