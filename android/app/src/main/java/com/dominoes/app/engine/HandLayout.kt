package com.dominoes.app.engine

import kotlin.math.max

data class HandLayoutMetrics(
    val tileWidth: Float,
    val tileHeight: Float,
    val gap: Float,
    val scale: Float
)

fun computeHandLayout(
    tileCount: Int,
    availableWidth: Float,
    baseWidth: Float,
    baseHeight: Float,
    baseGap: Float
): HandLayoutMetrics {
    if (tileCount <= 0 || availableWidth <= 0f) {
        return HandLayoutMetrics(baseWidth, baseHeight, baseGap, 1f)
    }

    val totalNormalWidth = tileCount * baseWidth + max(0, tileCount - 1) * baseGap
    if (totalNormalWidth <= availableWidth) {
        return HandLayoutMetrics(baseWidth, baseHeight, baseGap, 1f)
    }

    val scale = availableWidth / totalNormalWidth
    return HandLayoutMetrics(
        tileWidth = max(10f, baseWidth * scale),
        tileHeight = max(16f, baseHeight * scale),
        gap = max(1.5f, baseGap * scale),
        scale = scale
    )
}
