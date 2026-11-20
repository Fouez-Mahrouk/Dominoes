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
