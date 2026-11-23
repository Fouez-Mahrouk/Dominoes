/*
 * Dominoes
 * Copyright (c) 2026 Fouez Mahrouk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.dominoes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dominoes.app.R
import com.dominoes.app.model.ThemeMode

/*
 * Fonts: Fraunces (display serif) and Figtree (UI sans), both under the SIL
 * Open Font License 1.1 and bundled in res/font. Nothing is downloaded.
 */
val DisplayFamily: FontFamily = FontFamily(
    Font(R.font.fraunces_500, FontWeight.Medium),
    Font(R.font.fraunces_600, FontWeight.SemiBold)
)

val UiFamily: FontFamily = FontFamily(
    Font(R.font.figtree_400, FontWeight.Normal),
    Font(R.font.figtree_500, FontWeight.Medium),
    Font(R.font.figtree_700, FontWeight.Bold)
)

val DominoTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.08.em)
)

/** Tabular figures, for scores and counters. */
val ScoreStyle = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, fontFeatureSettings = "tnum")

/** Titles on the home and game bars. */
val GameTitleStyle = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)

/*
 * The field named accentTeal keeps its old name so the rest of the app keeps
 * compiling. Its value is now the amber accent.
 */
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
    val danger: Color,
    val onBoardWell: Color = Color(0xFFF6F0E4),
    val highContrast: Boolean = false,
    val borderWidth: Dp = 1.dp
)

// Warm paper, walnut ink, deep felt table, ivory tiles.
private val LightDominoPalette = DominoPalette(
    isDark = false,
    background = Color(0xFFFBF7EE),
    surfaceCard = Color(0xFFF3ECDD),
    surfaceElevated = Color(0xFFFFFCF5),
    boardWell = Color(0xFF174B3A),
    textPrimary = Color(0xFF1B1710),
    textSecondary = Color(0xFF6B6150),
    divider = Color(0x1F1B1710),
    accentTeal = Color(0xFFA85A00),
    onAccentTeal = Color(0xFFFFFFFF),
    tileBody = Color(0xFFF6EEDC),
    tileBorder = Color(0xFFCBBE9F),
    tileDivider = Color(0x401B1710),
    tilePip = Color(0xFF1B1710),
    tileBack = Color(0xFFE6DAC0),
    spinnerTint = Color(0xFFFFC233),
    amberHighlight = Color(0xFFB86E00),
    danger = Color(0xFFC62828)
)

// Warm near-black, lantern amber, dark felt, ivory tiles.
private val DarkDominoPalette = DominoPalette(
    isDark = true,
    background = Color(0xFF0B0A08),
    surfaceCard = Color(0xFF16130E),
    surfaceElevated = Color(0xFF201B14),
    boardWell = Color(0xFF123226),
    textPrimary = Color(0xFFF6F0E4),
    textSecondary = Color(0xFFB8AD98),
    divider = Color(0x1FFFF0D6),
    accentTeal = Color(0xFFFFC233),
    onAccentTeal = Color(0xFF1A1200),
    tileBody = Color(0xFFF1E6CF),
    tileBorder = Color(0xFF5A4B32),
    tileDivider = Color(0x40201A0E),
    tilePip = Color(0xFF1A1208),
    tileBack = Color(0xFFD9C9A8),
    spinnerTint = Color(0xFFFFC233),
    amberHighlight = Color(0xFFFFD15C),
    danger = Color(0xFFFF7A6B)
)

// High contrast: stronger lines (2dp), pure black/white text, colour-blind-safe avatars.
private val LightHighContrastPalette = DominoPalette(
    isDark = false,
    background = Color(0xFFFFFDF7),
    surfaceCard = Color(0xFFF1E9D8),
    surfaceElevated = Color(0xFFFFFFFF),
    boardWell = Color(0xFF123524),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF3D3626),
    divider = Color(0xB31B1710),
    accentTeal = Color(0xFF8A4500),
    onAccentTeal = Color(0xFFFFFFFF),
    tileBody = Color(0xFFFFFAEB),
    tileBorder = Color(0xFF1B1710),
    tileDivider = Color(0xA6000000),
    tilePip = Color(0xFF000000),
    tileBack = Color(0xFFE2D5B8),
    spinnerTint = Color(0xFFFFB300),
    amberHighlight = Color(0xFF8A4500),
    danger = Color(0xFFB00020),
    highContrast = true,
    borderWidth = 2.dp
)

private val DarkHighContrastPalette = DominoPalette(
    isDark = true,
    background = Color(0xFF000000),
    surfaceCard = Color(0xFF120F09),
    surfaceElevated = Color(0xFF1D1810),
    boardWell = Color(0xFF0F2A1D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE2D9C4),
    divider = Color(0xB3FFE9B8),
    accentTeal = Color(0xFFFFD24D),
    onAccentTeal = Color(0xFF000000),
    tileBody = Color(0xFFFFF6E0),
    tileBorder = Color(0xFFFFE9B8),
    tileDivider = Color(0xA6000000),
    tilePip = Color(0xFF000000),
    tileBack = Color(0xFFE6D6B4),
    spinnerTint = Color(0xFFFFD24D),
    amberHighlight = Color(0xFFFFE08A),
    danger = Color(0xFFFF9A8E),
    highContrast = true,
    borderWidth = 2.dp
)

val LocalDominoPalette = staticCompositionLocalOf { LightDominoPalette }

private val MaterialLightScheme = lightColorScheme(
    primary = Color(0xFFA85A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE7B8),
    onPrimaryContainer = Color(0xFF3A2000),
    secondary = Color(0xFF6B6150),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3ECDD),
    onSecondaryContainer = Color(0xFF1B1710),
    background = Color(0xFFFBF7EE),
    onBackground = Color(0xFF1B1710),
    surface = Color(0xFFFBF7EE),
    onSurface = Color(0xFF1B1710),
    surfaceVariant = Color(0xFFF3ECDD),
    onSurfaceVariant = Color(0xFF6B6150),
    outline = Color(0xFF8C8068),
    outlineVariant = Color(0xFFCBBE9F),
    error = Color(0xFFC62828)
)

private val MaterialDarkScheme = darkColorScheme(
    primary = Color(0xFFFFC233),
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF3D2C00),
    onPrimaryContainer = Color(0xFFFFE2A3),
    secondary = Color(0xFFB8AD98),
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF201B14),
    onSecondaryContainer = Color(0xFFF6F0E4),
    background = Color(0xFF0B0A08),
    onBackground = Color(0xFFF6F0E4),
    surface = Color(0xFF0B0A08),
    onSurface = Color(0xFFF6F0E4),
    surfaceVariant = Color(0xFF16130E),
    onSurfaceVariant = Color(0xFFB8AD98),
    outline = Color(0xFF8C8068),
    outlineVariant = Color(0xFF3A3326),
    error = Color(0xFFFF7A6B)
)

@Composable
fun DominoesTheme(
    themeMode: ThemeMode,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = when {
        useDark && highContrast -> DarkHighContrastPalette
        useDark -> DarkDominoPalette
        highContrast -> LightHighContrastPalette
        else -> LightDominoPalette
    }
    val baseScheme = if (useDark) MaterialDarkScheme else MaterialLightScheme
    val colorScheme = if (highContrast) {
        baseScheme.copy(
            primary = palette.accentTeal,
            onPrimary = palette.onAccentTeal,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.background,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.textPrimary,
            outlineVariant = palette.divider
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalDominoPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DominoTypography,
            content = content
        )
    }
}
