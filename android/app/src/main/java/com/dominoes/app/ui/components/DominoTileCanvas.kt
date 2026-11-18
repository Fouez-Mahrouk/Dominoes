package com.dominoes.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dominoes.app.engine.TileOrientation
import com.dominoes.app.model.DominoTile
import com.dominoes.app.ui.theme.LocalDominoPalette
import kotlin.math.min

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
        val corner = min(w, h) * 0.20f

        // Drop shadow for subtle 3D depth
        drawRoundRect(
            color = Color.Black.copy(alpha = (if (isSelected) 0.28f else 0.16f) * alpha),
            topLeft = Offset(0f, min(w, h) * 0.08f),
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Tile body
        drawRoundRect(
            color = palette.tileBody.copy(alpha = alpha),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle inner bevel highlight
        drawRoundRect(
            color = Color.White.copy(alpha = (if (palette.isDark) 0.28f else 0.10f) * alpha),
            topLeft = Offset(1.5f, 1.5f),
            size = Size((w - 3f).coerceAtLeast(1f), (h - 3f).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5f)
        )

        // Outer border / highlight ring
        val borderColor = when {
            isSelected -> palette.accentTeal
            isSpinner -> palette.spinnerTint
            isPlayable -> palette.accentTeal.copy(alpha = 0.75f)
            else -> palette.tileBorder
        }
        val strokeW = if (isSelected || isSpinner || isPlayable) 2.5.dp.toPx() else 1.dp.toPx()
        drawRoundRect(
            color = borderColor.copy(alpha = alpha),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeW)
        )

        // Divider groove & pips
        val pipColor = palette.tilePip.copy(alpha = alpha)
        val divColor = palette.tileDivider.copy(alpha = alpha)

        if (orientation == TileOrientation.VERTICAL) {
            val midY = h / 2f
            drawLine(
                color = divColor,
                start = Offset(w * 0.14f, midY),
                end = Offset(w * 0.86f, midY),
                strokeWidth = 1.5.dp.toPx().coerceAtLeast(1.2f)
            )
            drawHalfPips(tile.a, Offset(0f, 0f), Size(w, midY), pipColor)
            drawHalfPips(tile.b, Offset(0f, midY), Size(w, midY), pipColor)
        } else {
            val midX = w / 2f
            drawLine(
                color = divColor,
                start = Offset(midX, h * 0.14f),
                end = Offset(midX, h * 0.86f),
                strokeWidth = 1.5.dp.toPx().coerceAtLeast(1.2f)
            )
            drawHalfPips(tile.a, Offset(0f, 0f), Size(midX, h), pipColor)
            drawHalfPips(tile.b, Offset(midX, 0f), Size(midX, h), pipColor)
        }
    }
}

private fun DrawScope.drawHalfPips(
    value: Int,
    topLeft: Offset,
    halfSize: Size,
    color: Color
) {
    val minDim = min(halfSize.width, halfSize.height)
    val radius = (minDim * 0.105f).coerceAtLeast(1.5f)
    val padX = halfSize.width * 0.10f
    val padY = halfSize.height * 0.10f
    val innerW = halfSize.width - padX * 2f
    val innerH = halfSize.height - padY * 2f

    pipOffsets(value).forEach { (px, py) ->
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(
                x = topLeft.x + padX + innerW * px,
                y = topLeft.y + padY + innerH * py
            )
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
