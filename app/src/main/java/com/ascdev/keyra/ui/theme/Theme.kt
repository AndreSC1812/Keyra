package com.ascdev.keyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ascdev.keyra.core.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF22307F),
    onPrimaryContainer = Color(0xFFDDE1FF),
    inversePrimary = IndigoLight,
    secondary = Mint,
    onSecondary = Color(0xFF00201B),
    secondaryContainer = Color(0xFF0B3B36),
    onSecondaryContainer = Color(0xFF8FF5E2),
    tertiary = Amber,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = Color(0xFF3D2C05),
    onTertiaryContainer = Color(0xFFFFDEA6),
    background = Night,
    onBackground = NightOnSurface,
    surface = Night,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0B0E19),
    surfaceContainerLow = NightSurfaceLow,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    error = ErrorRed,
    onError = Color(0xFF3B0010),
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = MintDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F2E8),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = AmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2A8),
    onTertiaryContainer = Color(0xFF281800),
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DayBackground,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayOnSurfaceVariant,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F1FA),
    surfaceContainerHigh = DaySurfaceHigh,
    surfaceContainerHighest = Color(0xFFE1E4F3),
)

/** Mint marks safe states, amber marks warnings. */
val ColorScheme.safe: Color get() = secondary
val ColorScheme.warning: Color get() = tertiary

@Composable
fun isKeyraDark(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun KeyraTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = isKeyraDark(themeMode)
    MaterialTheme(
        colorScheme = if (dark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
