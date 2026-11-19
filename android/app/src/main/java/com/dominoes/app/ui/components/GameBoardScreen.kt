package com.dominoes.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dominoes.app.engine.BoardLayout
import com.dominoes.app.engine.DominoEngine
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.engine.computeHandLayout
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GamePhase
import com.dominoes.app.model.MatchState
import com.dominoes.app.model.PlayTarget
import com.dominoes.app.model.RoundWinCelebration
import com.dominoes.app.ui.theme.LocalDominoPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val SparkleAngles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)

@Composable
fun GameBoardScreen(
    match: MatchState,
    isAIThinking: Boolean,
    roundWinCelebration: RoundWinCelebration?,
    onPlayTile: (String, PlayTarget) -> Unit,
    onDrawTile: () -> Unit,
    onPassTurn: () -> Unit,
    onSelectTileHaptic: () -> Unit,
    onOpenRuleTip: () -> Unit,
    onRequestQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    val density = LocalDensity.current

    val currentPlayer = match.players[match.currentPlayerIndex]
    val isHumanTurn = !currentPlayer.isAI && match.phase == GamePhase.PLAYING
    val modeMeta = DominoEngine.metaFor(match.mode)
    val roundBased = DominoEngine.isRoundBasedMode(match.mode)

    var selectedTileId by remember { mutableStateOf<String?>(null) }
    var draggingTile by remember { mutableStateOf<DominoTile?>(null) }
    var dragRootCenter by remember { mutableStateOf(Offset.Zero) }
    var hoveredTarget by remember { mutableStateOf<PlayTarget?>(null) }
    var scoreCounter by remember { mutableStateOf(0) }
    var scoreEvent by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val scope = rememberCoroutineScope()
    val cameraX = remember { Animatable(0f) }
    val cameraY = remember { Animatable(0f) }
    val dropZoneCenters = remember { mutableStateMapOf<PlayTarget, Offset>() }

    LaunchedEffect(match.currentPlayerIndex, match.moveSequence, match.roundNumber) {
        selectedTileId = null
        draggingTile = null
        hoveredTarget = null
    }

    LaunchedEffect(match.lastScoringEvent?.id) {
        val event = match.lastScoringEvent
        if (event != null) {
            scoreCounter += 1
            scoreEvent = scoreCounter to "+${event.points}"
        }
    }

    val displayedPlayer = if (currentPlayer.isAI) match.players.first() else currentPlayer
    val layout = remember(match.board, match.mode) {
        BoardLayout.compute(match.board, match.mode)
    }

    LaunchedEffect(match.lastPlayedTileId, match.moveSequence, match.roundNumber) {
        val lastId = match.lastPlayedTileId
        if (match.board.root == null || lastId == null) {
            scope.launch { cameraX.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
            scope.launch { cameraY.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
        } else {
            val targetTile = layout.tiles.find { it.placed.tile.id == lastId }
            if (targetTile != null) {
                val targetX = -with(density) { targetTile.x.dp.toPx() }
                val targetY = -with(density) { targetTile.y.dp.toPx() }
                scope.launch { cameraX.animateTo(targetX, tween(300, easing = FastOutSlowInEasing)) }
                scope.launch { cameraY.animateTo(targetY, tween(300, easing = FastOutSlowInEasing)) }
            }
        }
    }
    val openEnds = remember(match.board, match.mode) {
        DominoEngine.getOpenEnds(match.board, match.mode)
    }
    val openEndsSum = remember(match.board, match.mode) {
        DominoEngine.getOpenEndsSum(match.board, match.mode)
    }
    val legalMoves = remember(isHumanTurn, currentPlayer.hand, match.board, match.mode) {
        if (isHumanTurn) DominoEngine.getLegalMoves(currentPlayer.hand, match.board, match.mode)
        else emptyList()
    }
    val playableTileIds = remember(legalMoves) { legalMoves.map { it.tile.id }.toSet() }

    val activeTile = remember(draggingTile, selectedTileId, currentPlayer.hand) {
        val id = draggingTile?.id ?: selectedTileId
        if (id == null) null else currentPlayer.hand.find { it.id == id }
    }

    val validTargetsForActiveTile = remember(activeTile, isHumanTurn, match.board.root, openEnds) {
        if (activeTile == null || !isHumanTurn) {
            emptySet()
        } else if (match.board.root == null) {
            setOf(PlayTarget.ROOT)
        } else {
            openEnds.filter { DominoEngine.tileCanMatchEnd(activeTile, it) }
                .map { PlayTarget.fromBranch(it.branch) }
                .toSet()
        }
    }

    fun findNearestValidTarget(rootPos: Offset, tile: DominoTile): PlayTarget? {
        val valid = if (match.board.root == null) {
            setOf(PlayTarget.ROOT)
        } else {
            openEnds.filter { DominoEngine.tileCanMatchEnd(tile, it) }
                .map { PlayTarget.fromBranch(it.branch) }
                .toSet()
        }
        val thresholdPx = with(density) { 80.dp.toPx() }
        var best: PlayTarget? = null
        var bestDist = thresholdPx
        valid.forEach { target ->
            val center = dropZoneCenters[target] ?: return@forEach
            val dist = hypot(rootPos.x - center.x, rootPos.y - center.y)
            if (dist < bestDist) {
                bestDist = dist
                best = target
            }
        }
        return best
    }

    fun handleHandTileTap(tile: DominoTile) {
        if (!isHumanTurn || !playableTileIds.contains(tile.id)) return
        onSelectTileHaptic()

        if (match.board.root == null) {
            onPlayTile(tile.id, PlayTarget.ROOT)
            return
        }

        val matchingEnds = openEnds.filter { DominoEngine.tileCanMatchEnd(tile, it) }
        if (selectedTileId == tile.id) {
            if (matchingEnds.size == 1) {
                onPlayTile(tile.id, PlayTarget.fromBranch(matchingEnds.first().branch))
            } else {
                selectedTileId = null
            }
        } else {
            selectedTileId = tile.id
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Header Row: Mode badge, Open ends sum, Help, Quit (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.surfaceCard,
                        border = BorderStroke(1.dp, palette.divider)
                    ) {
                        Text(
                            text = "${modeMeta.name} · R${match.roundNumber}",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (!roundBased && match.board.root != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.accentTeal.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Ends: $openEndsSum",
                                color = palette.accentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (modeMeta.hasBoneyard) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.surfaceCard
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Layers,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${match.boneyard.size}",
                                    color = palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenRuleTip,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Mode Rules",
                            tint = palette.textSecondary
                        )
                    }
                    IconButton(
                        onClick = onRequestQuit,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Quit Game",
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            // Player Score Bar (with Round Won! badge over winner avatar)
            PlayerScoreBar(
                mode = match.mode,
                players = match.players,
                currentPlayerIndex = match.currentPlayerIndex,
                targetScore = match.targetScore,
                isAIThinking = isAIThinking,
                lastDrawnTileId = match.lastDrawnTileId,
                lastDrawnPlayerId = match.lastDrawnPlayerId,
                roundWinnerId = roundWinCelebration?.winnerId
            )

            // Center Board Tabletop (Pan-only, scale fixed at 1.0f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.boardWell)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                cameraX.snapTo(cameraX.value + dragAmount.x)
                                cameraY.snapTo(cameraY.value + dragAmount.y)
                            }
                        }
                    }
            ) {
                // Empty board root drop target
                if (match.board.root == null) {
                    val isRootValid = validTargetsForActiveTile.contains(PlayTarget.ROOT)
                    val isRootHovered = hoveredTarget == PlayTarget.ROOT
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(110.dp, 58.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                dropZoneCenters[PlayTarget.ROOT] = Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f
                                )
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = isRootValid && activeTile != null) {
                                activeTile?.let { onPlayTile(it.id, PlayTarget.ROOT) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val dashColor = if (isRootHovered || isRootValid) palette.accentTeal else palette.textSecondary.copy(alpha = 0.45f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = dashColor,
                                style = Stroke(
                                    width = if (isRootHovered) 3.dp.toPx() else 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            )
                        }
                        Text(
                            text = if (isHumanTurn) "Play Here" else "${currentPlayer.name} opens",
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Panned board container centered at Box center (scale hardcoded to 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1f
                                scaleY = 1f
                                translationX = cameraX.value
                                translationY = cameraY.value
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Drop zones on valid endpoints
                        layout.dropZones.forEach { dz ->
                            val target = PlayTarget.fromBranch(dz.end.branch)
                            val isValid = validTargetsForActiveTile.contains(target)
                            val isHovered = hoveredTarget == target
                            if (isValid || isHovered) {
                                val dxDp = dz.x.dp
                                val dyDp = dz.y.dp
                                val wDp = dz.width.dp
                                val hDp = dz.height.dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = dxDp, y = dyDp)
                                        .size(wDp, hDp)
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            dropZoneCenters[target] = Offset(
                                                pos.x + coords.size.width / 2f,
                                                pos.y + coords.size.height / 2f
                                            )
                                        }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isHovered) palette.accentTeal.copy(alpha = 0.30f)
                                            else palette.accentTeal.copy(alpha = 0.14f)
                                        )
                                        .clickable(enabled = activeTile != null) {
                                            activeTile?.let { onPlayTile(it.id, target) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRoundRect(
                                            color = palette.accentTeal,
                                            style = Stroke(
                                                width = if (isHovered) 2.5.dp.toPx() else 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                            )
                                        )
                                    }
                                    Text(
                                        text = "${dz.end.requiredPip}",
                                        color = palette.accentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Placed tiles
                        layout.tiles.forEach { pos ->
                            key(pos.placed.tile.id) {
                                val isNew = match.lastPlayedTileId == pos.placed.tile.id
                                val txDp = pos.x.dp
                                val tyDp = pos.y.dp
                                val wDp = pos.width.dp
                                val hDp = pos.height.dp

                                val slideOffsetY = remember(pos.placed.tile.id) {
                                    Animatable(if (isNew) with(density) { (-22).dp.toPx() } else 0f)
                                }
                                val appearScale = remember(pos.placed.tile.id) {
                                    Animatable(if (isNew) 0.72f else 1f)
                                }
                                LaunchedEffect(pos.placed.tile.id) {
                                    if (isNew) {
                                        launch {
                                            slideOffsetY.animateTo(
                                                0f,
                                                tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        launch {
                                            appearScale.animateTo(
                                                1f,
                                                keyframes {
                                                    durationMillis = 400
                                                    0.72f at 0
                                                    1.12f at 260
                                                    1.0f at 400
                                                }
                                            )
                                        }
                                    }
                                }

                                DominoTileCanvas(
                                    tile = DominoTile(pos.placed.tile.id, pos.firstPip, pos.secondPip),
                                    orientation = pos.orientation,
                                    width = wDp,
                                    height = hDp,
                                    isSpinner = pos.placed.isSpinner,
                                    modifier = Modifier
                                        .offset(x = txDp, y = tyDp)
                                        .graphicsLayer {
                                            translationY = slideOffsetY.value
                                            scaleX = appearScale.value
                                            scaleY = appearScale.value
                                        }
                                )
                            }
                        }
                    }
                }

                // Subtle action banner at top of tabletop
                match.actionBanner?.let { banner ->
                    if (roundWinCelebration == null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.surfaceElevated.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, palette.divider),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = banner,
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Floating score text centered in board area with guaranteed 1.2s cleanup
                if (scoreEvent != null) {
                    key(scoreEvent!!.first) {
                        val scoreLabel = scoreEvent!!.second
                        val scoreAlpha = remember { Animatable(0f) }
                        val scoreTranslateY = remember { Animatable(20f) }

                        LaunchedEffect(Unit) {
                            launch {
                                scoreAlpha.animateTo(
                                    targetValue = 0f,
                                    animationSpec = keyframes {
                                        durationMillis = 1200
                                        0f at 0
                                        1f at 300
                                        1f at 900
                                        0f at 1200
                                    }
                                )
                            }
                            launch {
                                scoreTranslateY.animateTo(
                                    targetValue = -20f,
                                    animationSpec = keyframes {
                                        durationMillis = 1200
                                        20f at 0
                                        0f at 300
                                        0f at 900
                                        -20f at 1200
                                    }
                                )
                            }
                            delay(1200L)
                            scoreEvent = null
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .zIndex(25f)
                                .offset(y = scoreTranslateY.value.dp)
                                .graphicsLayer { alpha = scoreAlpha.value },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.accentTeal,
                                shadowElevation = 8.dp
                            ) {
                                Text(
                                    text = scoreLabel,
                                    color = palette.onAccentTeal,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Celebratory Round-Win Tabletop Overlay across all modes
                RoundWinCelebrationOverlay(
                    visible = roundWinCelebration != null,
                    celebration = roundWinCelebration,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(30f)
                )
            }

            // Human Turn Status / Draw / Pass Controls
            if (isHumanTurn && legalMoves.isEmpty()) {
                val canDraw = modeMeta.hasBoneyard && match.boneyard.isNotEmpty()
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (canDraw) "No matching tile in hand" else "No legal move available",
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = if (canDraw) onDrawTile else onPassTurn,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canDraw) palette.accentTeal else palette.textPrimary,
                                contentColor = if (canDraw) palette.onAccentTeal else palette.background
                            )
                        ) {
                            Icon(
                                imageVector = if (canDraw) Icons.Filled.Layers else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (canDraw) "Draw (${match.boneyard.size})" else "Pass Turn",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Human Player Hand — Proportional Auto-Scaling (No Overlap, No Scroll)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                border = BorderStroke(1.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val handLayout = computeHandLayout(
                        tileCount = displayedPlayer.hand.size,
                        availableWidth = maxWidth.value,
                        baseWidth = 46f,
                        baseHeight = 92f,
                        baseGap = 7f
                    )

                    val animW by animateDpAsState(
                        targetValue = handLayout.tileWidth.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "handW"
                    )
                    val animH by animateDpAsState(
                        targetValue = handLayout.tileHeight.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "handH"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(handLayout.gap.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayedPlayer.hand.forEach { tile ->
                            key(tile.id) {
                                val isPlayable = isHumanTurn && playableTileIds.contains(tile.id)
                                val isSelected = selectedTileId == tile.id
                                val isDraggingThis = draggingTile?.id == tile.id
                                val isNewDraw = displayedPlayer.id == match.lastDrawnPlayerId &&
                                    tile.id == match.lastDrawnTileId

                                val slideY = remember(tile.id, isNewDraw) {
                                    Animatable(if (isNewDraw) -70f else 0f)
                                }
                                val appearAlpha = remember(tile.id, isNewDraw) {
                                    Animatable(if (isNewDraw) 0f else 1f)
                                }
                                LaunchedEffect(isNewDraw) {
                                    if (isNewDraw) {
                                        launch { slideY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                        launch { appearAlpha.animateTo(1f, tween(240)) }
                                    }
                                }

                                var tileOriginRoot by remember { mutableStateOf(Offset.Zero) }

                                Box(
                                    modifier = Modifier
                                        .offset(y = if (isSelected) (-8).dp else 0.dp)
                                        .offset { IntOffset(0, slideY.value.roundToInt()) }
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            tileOriginRoot = Offset(
                                                pos.x + coords.size.width / 2f,
                                                pos.y + coords.size.height / 2f
                                            )
                                        }
                                        .graphicsLayer {
                                            alpha = if (isDraggingThis) 0.25f else appearAlpha.value
                                        }
                                        .clickable(enabled = isPlayable) {
                                            handleHandTileTap(tile)
                                        }
                                        .pointerInput(tile.id, isPlayable) {
                                            if (!isPlayable) return@pointerInput
                                            detectDragGestures(
                                                onDragStart = {
                                                    onSelectTileHaptic()
                                                    draggingTile = tile
                                                    selectedTileId = tile.id
                                                    dragRootCenter = tileOriginRoot
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragRootCenter += dragAmount
                                                    hoveredTarget = findNearestValidTarget(dragRootCenter, tile)
                                                },
                                                onDragEnd = {
                                                    val drop = findNearestValidTarget(dragRootCenter, tile)
                                                    if (drop != null) {
                                                        onPlayTile(tile.id, drop)
                                                    }
                                                    draggingTile = null
                                                    hoveredTarget = null
                                                },
                                                onDragCancel = {
                                                    draggingTile = null
                                                    hoveredTarget = null
                                                }
                                            )
                                        }
                                ) {
                                    DominoTileCanvas(
                                        tile = tile,
                                        orientation = TileOrientation.VERTICAL,
                                        width = animW,
                                        height = animH,
                                        isSelected = isSelected,
                                        isPlayable = isPlayable,
                                        isMuted = isHumanTurn && !isPlayable
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating dragged tile preview
        draggingTile?.let { tile ->
            val halfW = with(density) { 26.dp.toPx() }
            val halfH = with(density) { 52.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragRootCenter.x - halfW).roundToInt(),
                            (dragRootCenter.y - halfH).roundToInt()
                        )
                    }
                    .zIndex(50f)
            ) {
                DominoTileCanvas(
                    tile = tile,
                    orientation = TileOrientation.VERTICAL,
                    width = 52.dp,
                    height = 104.dp,
                    isSelected = true
                )
            }
        }
    }
}

@Composable
private fun SparkleBurstCanvas(color: Color, radiusDp: Float = 64f) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 900))
    }
    Canvas(modifier = Modifier.size((radiusDp * 2.4f).dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDist = radiusDp.dp.toPx()
        SparkleAngles.forEach { deg ->
            val rad = deg * (PI / 180.0)
            val dist = maxDist * progress.value
            val px = center.x + (cos(rad) * dist).toFloat()
            val py = center.y + (sin(rad) * dist).toFloat()
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 4.dp.toPx() * (1f - progress.value * 0.4f),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun RoundWinCelebrationOverlay(
    visible: Boolean,
    celebration: RoundWinCelebration?,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.78f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.92f) + fadeOut(),
        modifier = modifier
    ) {
        celebration?.let { cel ->
            Box(contentAlignment = Alignment.Center) {
                SparkleBurstCanvas(color = palette.amberHighlight, radiusDp = 82f)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.96f),
                    border = BorderStroke(1.5.dp, palette.amberHighlight),
                    shadowElevation = 14.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.amberHighlight.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = palette.amberHighlight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Round ${cel.roundNumber} Complete",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${cel.winnerName} Wins the Round!",
                                color = palette.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
