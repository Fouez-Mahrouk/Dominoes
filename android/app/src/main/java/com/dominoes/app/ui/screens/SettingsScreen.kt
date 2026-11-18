package com.dominoes.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominoes.app.model.Difficulty
import com.dominoes.app.model.GameSettings
import com.dominoes.app.model.LifetimeStats
import com.dominoes.app.model.ThemeMode
import com.dominoes.app.ui.theme.LocalDominoPalette

@Composable
fun SettingsScreen(
    settings: GameSettings,
    stats: LifetimeStats,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleVibration: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleModeTip: () -> Unit,
    onSelectDefaultDifficulty: (Difficulty) -> Unit,
    onResetScores: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = "Settings",
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Appearance, tactile feedback, AI behavior, and local match records",
                color = palette.textSecondary,
                fontSize = 13.sp
            )
        }

        // Theme Selector
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceCard,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = "Theme",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Warm off-white with charcoal tiles in light mode · Deep navy with ivory tiles in dark mode",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val active = settings.theme == mode
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Filled.PhoneAndroid
                            ThemeMode.LIGHT -> Icons.Filled.LightMode
                            ThemeMode.DARK -> Icons.Filled.DarkMode
                        }
                        Surface(
                            onClick = { onSelectTheme(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) palette.textPrimary else palette.background.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) palette.background else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = mode.label,
                                    color = if (active) palette.background else palette.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Haptics, Sound & Mode Tip
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceCard,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                SettingToggleRow(
                    icon = Icons.Filled.Vibration,
                    title = "Haptic Vibration",
                    subtitle = "Distinct pulses on tile pickup, placement, scoring, and victory",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { onToggleVibration() }
                )
                HorizontalDivider(color = palette.divider)
                SettingToggleRow(
                    icon = Icons.Filled.VolumeUp,
                    title = "Sound Effects",
                    subtitle = "Offline synthesized tile clacks, chimes, and win chords",
                    checked = settings.soundEnabled,
                    onCheckedChange = { onToggleSound() }
                )
                HorizontalDivider(color = palette.divider)
                SettingToggleRow(
                    icon = null,
                    title = "Mode Tip at Match Start",
                    subtitle = "Show a brief animated rule reminder before Round 1",
                    checked = settings.showModeTipOnStart,
                    onCheckedChange = { onToggleModeTip() }
                )
            }
        }

        // Default AI Difficulty
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceCard,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Default AI Difficulty",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val active = settings.difficulty == diff
                        Surface(
                            onClick = { onSelectDefaultDifficulty(diff) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) palette.textPrimary else palette.background.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = diff.label,
                                    color = if (active) palette.background else palette.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Career Records & Reset
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceCard,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Offline Career Records",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCell("Matches", "${stats.matchesPlayed}", palette.textPrimary)
                    StatCell("Wins", "${stats.matchesWon}", palette.accentTeal)
                    StatCell("Rounds", "${stats.roundsWon}", palette.textPrimary)
                    StatCell("Best Score", "${stats.highestScore}", palette.textPrimary)
                }

                HorizontalDivider(color = palette.divider)

                if (confirmReset) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onResetScores()
                                confirmReset = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.danger,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Confirm Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { confirmReset = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.background.copy(alpha = 0.7f),
                                contentColor = palette.textSecondary
                            )
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = { confirmReset = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.danger.copy(alpha = 0.12f),
                            contentColor = palette.danger
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Reset Scores & Saved Match", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalDominoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accentTeal,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onAccentTeal,
                checkedTrackColor = palette.accentTeal
            )
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color) {
    val palette = LocalDominoPalette.current
    Column {
        Text(text = label, color = palette.textSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
