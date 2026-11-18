package com.dominoes.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GameMode
import com.dominoes.app.ui.components.DominoTileCanvas
import com.dominoes.app.ui.theme.LocalDominoPalette

private data class ModeGuide(
    val id: GameMode,
    val tabTitle: String,
    val title: String,
    val tagline: String,
    val rules: List<String>,
    val exampleLabel: String
)

private val ModeGuides = listOf(
    ModeGuide(
        id = GameMode.FREE_GAME,
        tabTitle = "Free Game",
        title = "Free Game",
        tagline = "Open dominoes · Draw when stuck · Win by rounds",
        rules = listOf(
            "Each player is dealt 7 tiles (remaining tiles stay in the boneyard for 2- and 3-player games). The highest double opens the board.",
            "Take turns matching either open end of the chain. There is no scoring on multiples — the goal is to empty your hand.",
            "If you cannot play, draw from the boneyard until you can play or it runs empty. If empty and still stuck, pass your turn.",
            "When the boneyard is empty and everyone passes, the player with the lowest pip total wins the round. First to reach 1, 3, 5, or 7 round wins takes the match."
        ),
        exampleLabel = "Empty your hand first or hold lowest pips to win the round"
    ),
    ModeGuide(
        id = GameMode.BLOCK,
        tabTitle = "Block",
        title = "Block Dominoes",
        tagline = "Pure strategy · No drawing from boneyard",
        rules = listOf(
            "Each player starts with 7 tiles. Match the pip count on either open end of the chain.",
            "If you cannot play, you must Pass your turn.",
            "First player to empty their hand — or lowest pip total when blocked — wins 1 round.",
            "First player to reach the selected Rounds to Win target (1, 3, 5, or 7) wins the match."
        ),
        exampleLabel = "6|4 connects to 4|1 -> Open ends are 6 and 1"
    ),
    ModeGuide(
        id = GameMode.ALL_FIVES,
        tabTitle = "All Fives",
        title = "All Fives (Muggins)",
        tagline = "Draw when stuck · Score multiples of 5",
        rules = listOf(
            "Whenever the exposed open ends add up to a multiple of 5 (5, 10, 15, 20…), you immediately score that exact total.",
            "The first double played becomes a 4-way Spinner. Once both inline sides are covered, its top and bottom also open.",
            "If an exposed end tile is a double, both halves count toward the open total (e.g., 5|5 end counts as 10).",
            "If you have no match, draw from the boneyard until you find one or it runs empty."
        ),
        exampleLabel = "Open ends 4 + 6 = 10 -> +10 pts immediately"
    ),
    ModeGuide(
        id = GameMode.ALL_THREES,
        tabTitle = "All Threes",
        title = "All Threes",
        tagline = "Draw when stuck · Score multiples of 3",
        rules = listOf(
            "Plays just like All Fives with boneyard drawing and a 4-way Spinner.",
            "Score immediately whenever open ends sum to a multiple of 3 (3, 6, 9, 12, 15…).",
            "Round-end remaining pip difference is rounded to the nearest 3."
        ),
        exampleLabel = "Open ends 6 + 3 = 9 -> +9 pts immediately"
    ),
    ModeGuide(
        id = GameMode.FIVES_AND_THREES,
        tabTitle = "Fives & Threes",
        title = "Fives & Threes",
        tagline = "Draw when stuck · 1 pt per 5 and 1 pt per 3",
        rules = listOf(
            "Score 1 point for every 5 when open ends divide by 5, and 1 point for every 3 when open ends divide by 3.",
            "Hitting 15 scores both! (15 ÷ 5 = 3 pts plus 15 ÷ 3 = 5 pts -> 8 pts total).",
            "First player to reach the selected match target (50, 100, 150, or 200 points) wins immediately."
        ),
        exampleLabel = "Open ends total 15 -> 3 (fives) + 5 (threes) = +8 pts"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowToPlayScreen(modifier: Modifier = Modifier) {
    val palette = LocalDominoPalette.current
    var activeMode by remember { mutableStateOf(GameMode.FREE_GAME) }
    val guide = ModeGuides.first { it.id == activeMode }

    val infiniteTransition = rememberInfiniteTransition(label = "ruleDemo")
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demoTileSlide"
    )

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
                text = "How to Play",
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap a mode below to inspect scoring rules and animated tile examples",
                color = palette.textSecondary,
                fontSize = 13.sp
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeGuides.forEach { m ->
                val selected = m.id == activeMode
                Surface(
                    onClick = { activeMode = m.id },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) palette.accentTeal else palette.surfaceCard,
                    border = BorderStroke(1.dp, if (selected) palette.accentTeal else palette.divider)
                ) {
                    Text(
                        text = m.tabTitle,
                        color = if (selected) palette.onAccentTeal else palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

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
                Column {
                    Text(
                        text = guide.title,
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = guide.tagline,
                        color = palette.accentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Animated Mini-Board Example
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.boardWell,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (activeMode) {
                            GameMode.FREE_GAME, GameMode.BLOCK -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DominoTileCanvas(
                                        tile = DominoTile("d1", 6, 4),
                                        orientation = TileOrientation.HORIZONTAL,
                                        width = 56.dp,
                                        height = 28.dp
                                    )
                                    DominoTileCanvas(
                                        tile = DominoTile("d2", 4, 1),
                                        orientation = TileOrientation.HORIZONTAL,
                                        width = 56.dp,
                                        height = 28.dp,
                                        isPlayable = true,
                                        modifier = Modifier.offset(x = slideOffset.dp)
                                    )
                                }
                            }
                            GameMode.ALL_FIVES -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DominoTileCanvas(
                                        tile = DominoTile("f_top", 4, 5),
                                        orientation = TileOrientation.VERTICAL,
                                        width = 24.dp,
                                        height = 48.dp,
                                        modifier = Modifier.offset(y = (-slideOffset * 0.6f).dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DominoTileCanvas(
                                            tile = DominoTile("f_l", 2, 5),
                                            orientation = TileOrientation.HORIZONTAL,
                                            width = 48.dp,
                                            height = 24.dp
                                        )
                                        DominoTileCanvas(
                                            tile = DominoTile("f_s", 5, 5),
                                            orientation = TileOrientation.VERTICAL,
                                            width = 24.dp,
                                            height = 48.dp,
                                            isSpinner = true
                                        )
                                        DominoTileCanvas(
                                            tile = DominoTile("f_r", 5, 4),
                                            orientation = TileOrientation.HORIZONTAL,
                                            width = 48.dp,
                                            height = 24.dp
                                        )
                                    }
                                }
                            }
                            GameMode.ALL_THREES -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DominoTileCanvas(
                                        tile = DominoTile("t1", 6, 2),
                                        orientation = TileOrientation.HORIZONTAL,
                                        width = 52.dp,
                                        height = 26.dp
                                    )
                                    DominoTileCanvas(
                                        tile = DominoTile("t2", 2, 2),
                                        orientation = TileOrientation.VERTICAL,
                                        width = 26.dp,
                                        height = 52.dp,
                                        isSpinner = true
                                    )
                                    DominoTileCanvas(
                                        tile = DominoTile("t3", 2, 3),
                                        orientation = TileOrientation.HORIZONTAL,
                                        width = 52.dp,
                                        height = 26.dp,
                                        isPlayable = true,
                                        modifier = Modifier.offset(x = slideOffset.dp)
                                    )
                                }
                            }
                            GameMode.FIVES_AND_THREES -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DominoTileCanvas(
                                        tile = DominoTile("ft1", 6, 6),
                                        orientation = TileOrientation.VERTICAL,
                                        width = 26.dp,
                                        height = 52.dp,
                                        isSpinner = true
                                    )
                                    DominoTileCanvas(
                                        tile = DominoTile("ft2", 6, 3),
                                        orientation = TileOrientation.HORIZONTAL,
                                        width = 52.dp,
                                        height = 26.dp,
                                        isPlayable = true,
                                        modifier = Modifier.offset(x = slideOffset.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = guide.exampleLabel,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    guide.rules.forEachIndexed { idx, rule ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "0${idx + 1}.",
                                color = palette.accentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rule,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceCard,
            border = BorderStroke(1.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Touch Controls",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Drag any highlighted tile directly onto a matching open board end, or tap a tile once to illuminate valid endpoints and tap the target end to place. When only one valid endpoint exists, tapping a selected tile again places it automatically.",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
