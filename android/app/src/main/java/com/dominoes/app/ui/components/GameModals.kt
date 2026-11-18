package com.dominoes.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dominoes.app.engine.DominoEngine
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.MatchState
import com.dominoes.app.model.WinType
import com.dominoes.app.ui.theme.LocalDominoPalette

@Composable
fun PassDeviceDialog(
    playerName: String,
    onReady: () -> Unit
) {
    val palette = LocalDominoPalette.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(palette.accentTeal.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = palette.accentTeal
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Pass Device to $playerName",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep your tiles private. Tap below when $playerName is ready to play.",
                    color = palette.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentTeal,
                        contentColor = palette.onAccentTeal
                    )
                ) {
                    Text(
                        text = "Reveal $playerName's Hand",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ModeTipDialog(
    match: MatchState,
    onDismiss: () -> Unit
) {
    val palette = LocalDominoPalette.current
    val meta = DominoEngine.metaFor(match.mode)
    val roundBased = DominoEngine.isRoundBasedMode(match.mode)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val targetLabel = if (roundBased) {
                    "First to ${match.targetScore} ${if (match.targetScore == 1) "round" else "rounds"}"
                } else {
                    "Target ${match.targetScore} pts"
                }
                Text(
                    text = "${meta.name} · $targetLabel",
                    color = palette.accentTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quick Mode Primer",
                    color = palette.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meta.shortRule,
                    color = palette.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.boardWell,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DominoTileCanvas(
                                tile = DominoTile("ex1", 4, 6),
                                orientation = TileOrientation.HORIZONTAL,
                                width = 44.dp,
                                height = 22.dp
                            )
                            DominoTileCanvas(
                                tile = DominoTile("ex2", 6, 6),
                                orientation = TileOrientation.VERTICAL,
                                width = 22.dp,
                                height = 44.dp,
                                isSpinner = meta.hasSpinner
                            )
                            val rightPip = if (match.mode == GameMode.ALL_THREES || match.mode == GameMode.FIVES_AND_THREES) 5 else 1
                            DominoTileCanvas(
                                tile = DominoTile("ex3", 6, rightPip),
                                orientation = TileOrientation.HORIZONTAL,
                                width = 44.dp,
                                height = 22.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val exampleText = when (match.mode) {
                            GameMode.FREE_GAME -> "Draw when stuck · Empty hand or lowest pips wins"
                            GameMode.BLOCK -> "No drawing · Empty hand or lowest pips wins"
                            GameMode.ALL_FIVES -> "Open ends 4 + 1 = 5 -> +5 pts immediately"
                            GameMode.ALL_THREES -> "Open ends 4 + 5 = 9 -> +9 pts immediately"
                            GameMode.FIVES_AND_THREES -> "Open ends 4 + 5 = 9 -> 3×3 = +3 pts"
                        }
                        Text(
                            text = exampleText,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentTeal,
                        contentColor = palette.onAccentTeal
                    )
                ) {
                    Text(text = "Start Playing", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoundSummaryDialog(
    match: MatchState,
    onNextRound: () -> Unit
) {
    val summary = match.roundSummary ?: return
    val palette = LocalDominoPalette.current
    val roundBased = DominoEngine.isRoundBasedMode(match.mode)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(palette.accentTeal.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MilitaryTech,
                            contentDescription = null,
                            tint = palette.accentTeal
                        )
                    }
                    Column {
                        val winTypeStr = when (summary.winType) {
                            WinType.DOMINO -> "Domino Out"
                            WinType.BLOCKED -> "Blocked Board"
                            WinType.TIE -> "Tie"
                        }
                        Text(
                            text = "Round ${summary.roundNumber} Complete · $winTypeStr",
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                        val title = when {
                            summary.winnerId == null -> "Round Tied"
                            roundBased -> "${summary.winnerName} wins the round"
                            else -> "${summary.winnerName} wins +${summary.pointsAwarded} pts"
                        }
                        Text(
                            text = title,
                            color = palette.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                summary.playerBreakdowns.forEachIndexed { idx, b ->
                    val playerObj = match.players.find { it.id == b.playerId }
                    if (idx > 0) HorizontalDivider(color = palette.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = b.name,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (b.remainingTiles.isEmpty()) {
                                    "Empty hand (0 pips)"
                                } else {
                                    "${b.remainingTiles.size} tiles · ${b.remainingPips} pips left"
                                },
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = if (roundBased) {
                                "${playerObj?.roundsWon ?: 0} / ${match.targetScore}"
                            } else {
                                "${b.totalScore} / ${match.targetScore}"
                            },
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNextRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentTeal,
                        contentColor = palette.onAccentTeal
                    )
                ) {
                    Text(
                        text = "Continue to Round ${summary.roundNumber + 1}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    match: MatchState,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val palette = LocalDominoPalette.current
    val roundBased = DominoEngine.isRoundBasedMode(match.mode)
    val sorted = match.players.sortedByDescending { if (roundBased) it.roundsWon else it.score }
    val winner = match.players.find { it.id == match.matchWinnerId } ?: sorted.first()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(palette.amberHighlight.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = palette.amberHighlight,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val targetDesc = if (roundBased) {
                    "${match.targetScore} ${if (match.targetScore == 1) "round" else "rounds"} target"
                } else {
                    "${match.targetScore} pt target"
                }
                Text(
                    text = "Game Over · ${DominoEngine.metaFor(match.mode).name} ($targetDesc)",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${winner.name} Wins!",
                    color = palette.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (roundBased) {
                        "Rounds Won: ${winner.roundsWon} / ${match.targetScore}"
                    } else {
                        "Final Score: ${winner.score} pts"
                    },
                    color = palette.accentTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        sorted.forEachIndexed { idx, p ->
                            if (idx > 0) HorizontalDivider(color = palette.divider)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        color = palette.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = p.name,
                                        color = palette.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (roundBased) {
                                        "${p.roundsWon} ${if (p.roundsWon == 1) "round" else "rounds"}"
                                    } else {
                                        "${p.score} pts"
                                    },
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackToMenu,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceCard,
                            contentColor = palette.textPrimary
                        )
                    ) {
                        Text("Mode Menu", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentTeal,
                            contentColor = palette.onAccentTeal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Play Again", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuitGameDialog(
    onCancel: () -> Unit,
    onConfirmQuit: () -> Unit
) {
    val palette = LocalDominoPalette.current
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Quit this game?",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All progress will be lost.",
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceCard,
                            contentColor = palette.textPrimary
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirmQuit,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.danger,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Quit", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
