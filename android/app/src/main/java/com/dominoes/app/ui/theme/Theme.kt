package com.dominoes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dominoes.app.model.ThemeMode

data class DominoPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val boardWell: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val accentTeal: Color,
    val onAccentTeal: Color,
    val tileBody: Color,
    val tileBorder: Color,
    val tileDivider: Color,
    val tilePip: Color,
    val tileBack: Color,
    val spinnerTint: Color,
    val amberHighlight: Color,
    val danger: Color
)

private val LightDominoPalette = DominoPalette(
    isDark = false,
    background = Color(0xFFFAF8F4),
    surfaceCard = Color(0xFFF2EEE6),
    surfaceElevated = Color(0xFFFFFFFF),
    boardWell = Color(0xFF2E7D32),
    textPrimary = Color(0xFF171A21),
    textSecondary = Color(0xFF5A606D),
    divider = Color(0x14000000),
    accentTeal = Color(0xFF0F766E),
    onAccentTeal = Color(0xFFFFFFFF),
    tileBody = Color(0xFFF0EDE6),
    tileBorder = Color(0xFFC0BEB8),
    tileDivider = Color(0x331A1A1A),
    tilePip = Color(0xFF1A1A1A),
    tileBack = Color(0xFFE5E1D8),
    spinnerTint = Color(0xFFFFD600),
    amberHighlight = Color(0xFFFBBF24),
    danger = Color(0xFFE11D48)
)

private val DarkDominoPalette = DominoPalette(
    isDark = true,
    background = Color(0xFF0E131D),
    surfaceCard = Color(0xFF161E2E),
    surfaceElevated = Color(0xFF1E2738),
    boardWell = Color(0xFF1B2631),
    textPrimary = Color(0xFFF5F2EB),
    textSecondary = Color(0xFF9AA5B8),
    divider = Color(0x1AFFFFFF),
    accentTeal = Color(0xFF2DD4BF),
    onAccentTeal = Color(0xFF06201E),
    tileBody = Color(0xFFE8D5B7),
    tileBorder = Color(0xFFBAAA92),
    tileDivider = Color(0x330D1B2A),
    tilePip = Color(0xFF0D1B2A),
    tileBack = Color(0xFFDECBB0),
    spinnerTint = Color(0xFFFFD600),
    amberHighlight = Color(0xFFFCD34D),
    danger = Color(0xFFFB7185)
)

val LocalDominoPalette = staticCompositionLocalOf { LightDominoPalette }

private val MaterialLightScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    background = Color(0xFFFAF8F4),
    onBackground = Color(0xFF171A21),
    surface = Color(0xFFFAF8F4),
    onSurface = Color(0xFF171A21),
    surfaceVariant = Color(0xFFF2EEE6),
    onSurfaceVariant = Color(0xFF5A606D),
    error = Color(0xFFE11D48)
)

private val MaterialDarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF06201E),
    background = Color(0xFF0E131D),
    onBackground = Color(0xFFF5F2EB),
    surface = Color(0xFF0E131D),
    onSurface = Color(0xFFF5F2EB),
    surfaceVariant = Color(0xFF161E2E),
    onSurfaceVariant = Color(0xFF9AA5B8),
    error = Color(0xFFFB7185)
)

@Composable
fun DominoesTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (useDark) DarkDominoPalette else LightDominoPalette
    val colorScheme = if (useDark) MaterialDarkScheme else MaterialLightScheme

    CompositionLocalProvider(LocalDominoPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
