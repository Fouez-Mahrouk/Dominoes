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


package com.dominoes.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dominoes.app.engine.DominoEngine
import com.dominoes.app.engine.computeHandLayout
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.Player
import com.dominoes.app.ui.theme.LocalDominoPalette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val AvatarTones = listOf(
    Color(0xFF0F766E),
    Color(0xFFD97706),
    Color(0xFF4F46E5),
    Color(0xFFE11D48)
)

private val CelebrationAngles = listOf(0f, 60f, 120f, 180f, 240f, 300f)

@Composable
fun PlayerScoreBar(
    mode: GameMode,
    players: List<Player>,
    currentPlayerIndex: Int,
    targetScore: Int,
    isAIThinking: Boolean,
    lastDrawnTileId: String?,
    lastDrawnPlayerId: Int?,
    roundWinnerId: Int? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    val roundBased = DominoEngine.isRoundBasedMode(mode)
    val columns = if (players.size == 3) 3 else 2

    val rows = players.chunked(columns)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPlayers.forEach { player ->
                    val idx = players.indexOfFirst { it.id == player.id }
                    val isActive = idx == currentPlayerIndex
                    val isRoundWinner = roundWinnerId != null && player.id == roundWinnerId
                    val currentValue = if (roundBased) player.roundsWon else player.score
                    val progress = (currentValue.toFloat() / targetScore.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val tone = AvatarTones[player.avatarIndex % AvatarTones.size]

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .zIndex(if (isRoundWinner) 10f else 0f),
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isRoundWinner -> if (palette.isDark) Color(0xFF332611) else Color(0xFFFFFBEB)
                            isActive -> palette.surfaceElevated
                            else -> palette.surfaceCard.copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            width = if (isRoundWinner || isActive) 1.5.dp else 1.dp,
                            color = when {
                                isRoundWinner -> palette.amberHighlight
                                isActive -> palette.accentTeal.copy(alpha = 0.7f)
                                else -> palette.divider
                            }
                        ),
                        shadowElevation = if (isRoundWinner) 6.dp else if (isActive) 2.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(tone),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (player.isAI) Icons.Filled.SmartToy else Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    if (isActive && player.isAI && isAIThinking && !isRoundWinner) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.accentTeal
                                        )
                                    }

                                    // 6-particle burst & "Round Won!" badge over winner's avatar
                                    if (isRoundWinner) {
                                        WinnerAvatarParticles(color = palette.amberHighlight)
                                    }

                                    RoundWonBadge(
                                        visible = isRoundWinner,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-14).dp)
                                            .zIndex(20f)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = player.name,
                                                color = palette.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isActive && player.isAI && isAIThinking && !isRoundWinner) {
                                                ThinkingPulseDot(color = palette.accentTeal)
                                            }
                                        }
                                        Text(
                                            text = "$currentValue/$targetScore",
                                            color = palette.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = if (isActive && player.isAI && isAIThinking) {
                                            "Thinking..."
                                        } else {
                                            "${player.hand.size} ${if (player.hand.size == 1) "tile" else "tiles"}"
                                        },
                                        color = palette.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (player.isAI || idx != currentPlayerIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OpponentMiniHand(
                                    player = player,
                                    lastDrawnTileId = lastDrawnTileId,
                                    lastDrawnPlayerId = lastDrawnPlayerId
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (isActive) palette.accentTeal else palette.textSecondary.copy(alpha = 0.45f),
                                trackColor = palette.divider
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingPulseDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "aiThinkingDot")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiThinkingDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun WinnerAvatarParticles(color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 850))
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        CelebrationAngles.forEach { deg ->
            val rad = deg * (PI / 180.0)
            val dist = 24.dp.toPx() * progress.value
            val px = center.x + (cos(rad) * dist).toFloat()
            val py = center.y + (sin(rad) * dist * 0.85).toFloat()
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 3.dp.toPx() * (1f - progress.value * 0.5f),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun RoundWonBadge(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.55f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.85f) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.amberHighlight,
            border = BorderStroke(1.5.dp, palette.background),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "Round Won!",
                color = Color(0xFF0F172A),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun OpponentMiniHand(
    player: Player,
    lastDrawnTileId: String?,
    lastDrawnPlayerId: Int?
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val layout = computeHandLayout(
            tileCount = player.hand.size,
            availableWidth = maxWidth.value,
            baseWidth = 12f,
            baseHeight = 20f,
            baseGap = 3f
        )

        val animatedWidth by animateDpAsState(
            targetValue = layout.tileWidth.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "oppTileWidth"
        )
        val animatedHeight by animateDpAsState(
            targetValue = layout.tileHeight.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "oppTileHeight"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(layout.gap.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            player.hand.forEach { tile ->
                key(tile.id) {
                    val isNewDraw = player.id == lastDrawnPlayerId && tile.id == lastDrawnTileId
                    val slideY = remember(tile.id, isNewDraw) {
                        Animatable(if (isNewDraw) -26f else 0f)
                    }
                    val alpha = remember(tile.id, isNewDraw) {
                        Animatable(if (isNewDraw) 0f else 1f)
                    }
                    LaunchedEffect(isNewDraw) {
                        if (isNewDraw) {
                            scope.launch { slideY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            scope.launch { alpha.animateTo(1f, tween(220)) }
                        }
                    }
                    DominoTileBackCanvas(
                        width = animatedWidth,
                        height = animatedHeight,
                        modifier = Modifier
                            .offset { IntOffset(0, slideY.value.roundToInt()) }
                            .graphicsLayer { this.alpha = alpha.value }
                    )
                }
            }
        }
    }
}
