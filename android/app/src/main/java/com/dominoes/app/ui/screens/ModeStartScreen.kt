package com.dominoes.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominoes.app.engine.DominoEngine
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.model.Difficulty
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.MatchState
import com.dominoes.app.ui.components.DominoTileCanvas
import com.dominoes.app.ui.theme.LocalDominoPalette

private val SecondaryModes = listOf(
    GameMode.BLOCK,
    GameMode.ALL_FIVES,
    GameMode.ALL_THREES,
    GameMode.FIVES_AND_THREES
)

@Composable
fun ModeStartScreen(
    savedMatch: MatchState?,
    defaultDifficulty: Difficulty,
    onResumeMatch: () -> Unit,
    onDiscardSavedMatch: () -> Unit,
    onStartMatch: (GameMode, Int, Boolean, Int, Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    var selectedMode by remember { mutableStateOf(GameMode.FREE_GAME) }
    var playerCount by remember { mutableIntStateOf(2) }
    var passAndPlay by remember { mutableStateOf(false) }
    var targetScore by remember { mutableIntStateOf(3) }
    var difficulty by remember(defaultDifficulty) { mutableStateOf(defaultDifficulty) }

    val roundBased = DominoEngine.isRoundBasedMode(selectedMode)
    val targetOptions = if (roundBased) {
        DominoEngine.ROUND_TARGET_OPTIONS
    } else {
        DominoEngine.POINT_TARGET_OPTIONS
    }

    fun selectMode(mode: GameMode) {
        selectedMode = mode
        val nextRoundBased = DominoEngine.isRoundBasedMode(mode)
        if (nextRoundBased && targetScore !in DominoEngine.ROUND_TARGET_OPTIONS) {
            targetScore = 3
        } else if (!nextRoundBased && targetScore !in DominoEngine.POINT_TARGET_OPTIONS) {
            targetScore = 100
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Centered Title ("Dominoes" only)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                DominoTileCanvas(
                    tile = DominoTile("logo-5-3", 5, 3),
                    orientation = TileOrientation.VERTICAL,
                    width = 20.dp,
                    height = 40.dp
                )
                Text(
                    text = "Dominoes",
                    color = palette.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Resume Unfinished Match Card
            if (savedMatch != null) {
                val savedRoundBased = DominoEngine.isRoundBasedMode(savedMatch.mode)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    border = BorderStroke(1.dp, palette.accentTeal.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${DominoEngine.metaFor(savedMatch.mode).name} · R${savedMatch.roundNumber}",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val scoresStr = savedMatch.players.joinToString(" · ") { p ->
                                if (savedRoundBased) "${p.name} ${p.roundsWon}" else "${p.name} ${p.score}"
                            }
                            Text(
                                text = scoresStr,
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onDiscardSavedMatch,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Discard saved match",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Button(
                                onClick = onResumeMatch,
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentTeal,
                                    contentColor = palette.onAccentTeal
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Mode Picker: Free Game full width first, then 2x2 grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mode",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                ModeOptionButton(
                    label = DominoEngine.metaFor(GameMode.FREE_GAME).name,
                    selected = selectedMode == GameMode.FREE_GAME,
                    onClick = { selectMode(GameMode.FREE_GAME) },
                    isFeatured = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryModes.chunked(2).forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModes.forEach { mode ->
                            ModeOptionButton(
                                label = DominoEngine.metaFor(mode).name,
                                selected = selectedMode == mode,
                                onClick = { selectMode(mode) },
                                isFeatured = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Match Options Card: Players, Target, AI Level
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                border = BorderStroke(1.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Players
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "Players",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2, 3, 4).forEach { count ->
                                SelectorChip(
                                    label = "$count Players",
                                    selected = playerCount == count,
                                    onClick = {
                                        playerCount = count
                                        if (count != 4) passAndPlay = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (playerCount == 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AccentChip(
                                    label = "1 Human + 3 AI",
                                    selected = !passAndPlay,
                                    onClick = { passAndPlay = false },
                                    modifier = Modifier.weight(1f)
                                )
                                AccentChip(
                                    label = "Pass & Play",
                                    selected = passAndPlay,
                                    onClick = { passAndPlay = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Dynamic Target Picker ("Rounds to Win" vs "Target Score")
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (roundBased) "Rounds to Win" else "Target Score",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            targetOptions.forEach { value ->
                                SelectorChip(
                                    label = "$value",
                                    selected = targetScore == value,
                                    onClick = { targetScore = value },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // AI Difficulty
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "AI Level",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Difficulty.entries.forEach { diff ->
                                SelectorChip(
                                    label = diff.label,
                                    selected = difficulty == diff,
                                    onClick = { difficulty = diff },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Game CTA
            Button(
                onClick = {
                    onStartMatch(
                        selectedMode,
                        playerCount,
                        playerCount == 4 && passAndPlay,
                        targetScore,
                        difficulty
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentTeal,
                    contentColor = palette.onAccentTeal
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Start Game",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ModeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isFeatured: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (isFeatured) 48.dp else 44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) palette.textPrimary else palette.surfaceCard,
        border = BorderStroke(1.dp, if (selected) palette.textPrimary else palette.divider),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (selected) palette.background else palette.textPrimary,
                fontSize = if (isFeatured) 17.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.textPrimary else palette.background.copy(alpha = 0.65f),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (selected) palette.background else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.accentTeal else palette.background.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (selected) palette.onAccentTeal else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
