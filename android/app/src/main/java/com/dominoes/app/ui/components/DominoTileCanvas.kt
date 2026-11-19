package com.dominoes.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.model.DominoTile
import com.dominoes.app.ui.theme.LocalDominoPalette
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

private fun pipOffsets(value: Int): List<Pair<Float, Float>> = when (value) {
    1 -> listOf(0.5f to 0.5f)
    2 -> listOf(0.26f to 0.26f, 0.74f to 0.74f)
    3 -> listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f)
    4 -> listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.26f to 0.74f, 0.74f to 0.74f)
    5 -> listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.5f to 0.5f, 0.26f to 0.74f, 0.74f to 0.74f)
    6 -> listOf(
        0.26f to 0.23f, 0.74f to 0.23f,
        0.26f to 0.50f, 0.74f to 0.50f,
        0.26f to 0.77f, 0.74f to 0.77f
    )
    else -> emptyList()
}

@Composable
fun DominoTileCanvas(
    tile: DominoTile,
    orientation: TileOrientation,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    isSpinner: Boolean = false,
    isSelected: Boolean = false,
    isPlayable: Boolean = false,
    isMuted: Boolean = false
) {
    val palette = LocalDominoPalette.current
    val alpha = if (isMuted) 0.48f else 1f

    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val minSide = min(w, h)
        val corner = min(8.dp.toPx(), minSide * 0.22f)
        val edgeHeight = min(3.dp.toPx(), h * 0.12f)
        val faceHeight = (h - edgeHeight).coerceAtLeast(1f)

        val faceTop = if (palette.isDark) Color(0xFFFFF8E1) else Color(0xFFFFFFFF)
        val faceBottom = if (palette.isDark) Color(0xFFE8D5B7) else Color(0xFFF0EDE6)
        val edgeColor = if (palette.isDark) Color(0xFFC4A87A) else Color(0xFFD5CFC3)
        val subtleBorderColor = if (palette.isDark) Color(0xFF37474F) else Color(0xFFD5CFC3)
        val pipColor = (if (palette.isDark) Color(0xFF0D1B2A) else Color(0xFF1A1A1A)).copy(alpha = alpha)
        val divColor = (if (palette.isDark) Color(0xFF0D1B2A) else Color(0xFF1A1A1A)).copy(alpha = 0.22f * alpha)

        // Soft, wide ambient shadow (6dp elevation, ~8dp blur spread, 25% light / 40% dark)
        val targetShadowAlpha = (
            (if (palette.isDark) 0.40f else 0.25f) *
                (if (isSelected) 1.15f else 1f) *
                alpha
            ).coerceIn(0f, 0.99f)
        if (targetShadowAlpha > 0.001f) {
            val shadowLayers = 10
            val blurSpread = 8.dp.toPx()
            val elevationY = 6.dp.toPx()
            var sumWeight = 0f
            for (i in 1..shadowLayers) {
                sumWeight += (i.toFloat() / shadowLayers.toFloat()).pow(1.5f)
            }
            val totalLog = -ln(1f - targetShadowAlpha)
            for (i in 0 until shadowLayers) {
                val progress = (i + 1).toFloat() / shadowLayers.toFloat()
                val spread = blurSpread * (1f - i.toFloat() / shadowLayers.toFloat())
                val layerWeight = progress.pow(1.5f)
                val layerAlpha = (1f - exp(-totalLog * (layerWeight / sumWeight))).coerceIn(0f, 1f)
                val yShift = elevationY * (0.45f + 0.35f * (1f - progress))
                drawRoundRect(
                    color = Color.Black.copy(alpha = layerAlpha),
                    topLeft = Offset(-spread, -spread * 0.6f + yShift),
                    size = Size(w + spread * 2f, h + spread * 1.6f),
                    cornerRadius = CornerRadius(corner + spread * 0.45f, corner + spread * 0.45f)
                )
            }
        }

        // Physical 3dp bottom edge / thickness (15% darker than tile face bottom)
        drawRoundRect(
            color = edgeColor.copy(alpha = alpha),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Tile face with top-down warm ivory vertical gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(faceTop.copy(alpha = alpha), faceBottom.copy(alpha = alpha)),
                startY = 0f,
                endY = faceHeight
            ),
            topLeft = Offset.Zero,
            size = Size(w, faceHeight),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Border: 1dp subtle border (20% darker than tile face bottom), with selection/playable cue in hand
        val borderColor = when {
            isSelected -> palette.accentTeal
            isPlayable -> palette.accentTeal.copy(alpha = 0.70f)
            else -> subtleBorderColor
        }
        val strokeW = when {
            isSelected -> 2.dp.toPx()
            isPlayable -> 1.5.dp.toPx()
            else -> 1.dp.toPx()
        }
        val halfStroke = strokeW / 2f
        drawRoundRect(
            color = borderColor.copy(alpha = alpha),
            topLeft = Offset(halfStroke, halfStroke),
            size = Size((w - strokeW).coerceAtLeast(1f), (h - strokeW).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeW)
        )

        // Divider groove & carved pips on the top face
        if (orientation == TileOrientation.VERTICAL) {
            val midY = faceHeight / 2f
            drawLine(
                color = divColor,
                start = Offset(w * 0.14f, midY),
                end = Offset(w * 0.86f, midY),
                strokeWidth = 1.5.dp.toPx().coerceAtLeast(1.2f)
            )
            drawHalfPips(tile.a, Offset(0f, 0f), Size(w, midY), pipColor, alpha)
            drawHalfPips(tile.b, Offset(0f, midY), Size(w, midY), pipColor, alpha)
        } else {
            val midX = w / 2f
            drawLine(
                color = divColor,
                start = Offset(midX, faceHeight * 0.14f),
                end = Offset(midX, faceHeight * 0.86f),
                strokeWidth = 1.5.dp.toPx().coerceAtLeast(1.2f)
            )
            drawHalfPips(tile.a, Offset(0f, 0f), Size(midX, faceHeight), pipColor, alpha)
            drawHalfPips(tile.b, Offset(midX, 0f), Size(midX, faceHeight), pipColor, alpha)
        }

        // Spinner indicator: 8dp yellow dot (#FFD600) at 80% opacity centered on the spinner tile face
        if (isSpinner) {
            val dotDiameter = min(8.dp.toPx(), minSide * 0.40f)
            drawCircle(
                color = Color(0xFFFFD600).copy(alpha = 0.80f * alpha),
                radius = dotDiameter / 2f,
                center = Offset(w / 2f, faceHeight / 2f)
            )
        }
    }
}

private fun DrawScope.drawHalfPips(
    value: Int,
    topLeft: Offset,
    halfSize: Size,
    color: Color,
    tileAlpha: Float
) {
    val minDim = min(halfSize.width, halfSize.height)
    val radius = (minDim * 0.105f * 1.15f * 0.90f).coerceAtLeast(1.5f)
    val padX = halfSize.width * 0.10f
    val padY = halfSize.height * 0.10f
    val innerW = halfSize.width - padX * 2f
    val innerH = halfSize.height - padY * 2f
    val innerShadowStroke = min(1.dp.toPx(), radius * 0.55f)
    val arcSize = Size(
        width = (radius * 2f - innerShadowStroke * 0.5f).coerceAtLeast(1f),
        height = (radius * 2f - innerShadowStroke * 0.5f).coerceAtLeast(1f)
    )

    pipOffsets(value).forEach { (px, py) ->
        val center = Offset(
            x = topLeft.x + padX + innerW * px,
            y = topLeft.y + padY + innerH * py
        )
        drawCircle(
            color = color,
            radius = radius,
            center = center
        )
        drawArc(
            color = Color.Black.copy(alpha = 0.12f * tileAlpha),
            startAngle = 135f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                x = center.x - arcSize.width / 2f,
                y = center.y - arcSize.height / 2f
            ),
            size = arcSize,
            style = Stroke(width = innerShadowStroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun DominoTileBackCanvas(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalDominoPalette.current
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val corner = min(w, h) * 0.22f
        drawRoundRect(
            color = palette.tileBack,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = palette.tileBorder.copy(alpha = 0.7f),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.dp.toPx())
        )
        drawLine(
            color = palette.tileDivider,
            start = Offset(w * 0.2f, h / 2f),
            end = Offset(w * 0.8f, h / 2f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
