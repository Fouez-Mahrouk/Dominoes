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

import com.dominoes.app.model.BoardBranches
import com.dominoes.app.model.BranchDir
import com.dominoes.app.model.Direction
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.PlacedTile
import com.dominoes.app.model.TileId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Heading { E, W, N, S }

enum class TileOrientation { HORIZONTAL, VERTICAL }

data class PositionedTile(
    val placed: PlacedTile,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val orientation: TileOrientation,
    val firstPip: Int,
    val secondPip: Int
)

data class PositionedDropZone(
    val end: com.dominoes.app.model.OpenEnd,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val heading: Heading
)

data class BoardBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val width: Float,
    val height: Float,
    val halfSpanX: Float,
    val halfSpanY: Float
)

data class BoardLayoutResult(
    val tiles: List<PositionedTile>,
    val dropZones: List<PositionedDropZone>,
    val hasSpinner: Boolean,
    val bounds: BoardBounds
)

object BoardLayout {
    const val TILE_LONG = 84f
    const val TILE_SHORT = 42f
    const val TILE_GAP = 3f

    private data class TileDims(
        val width: Float,
        val height: Float,
        val orientation: TileOrientation
    )

    private data class PlacedRect(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    )

    private fun BranchDir.toDirection(): Direction = when (this) {
        BranchDir.LEFT -> Direction.LEFT
        BranchDir.RIGHT -> Direction.RIGHT
        BranchDir.UP -> Direction.UP
        BranchDir.DOWN -> Direction.DOWN
    }

    private fun Direction.toHeading(): Heading = when (this) {
        Direction.LEFT -> Heading.W
        Direction.RIGHT -> Heading.E
        Direction.UP -> Heading.N
        Direction.DOWN -> Heading.S
    }

    private fun tileDimensions(heading: Heading, isDouble: Boolean): TileDims {
        val isHorizontalTravel = heading == Heading.E || heading == Heading.W
        val orientation = if (isDouble) {
            if (isHorizontalTravel) TileOrientation.VERTICAL else TileOrientation.HORIZONTAL
        } else {
            if (isHorizontalTravel) TileOrientation.HORIZONTAL else TileOrientation.VERTICAL
        }
        return TileDims(
            width = if (orientation == TileOrientation.HORIZONTAL) TILE_LONG else TILE_SHORT,
            height = if (orientation == TileOrientation.HORIZONTAL) TILE_SHORT else TILE_LONG,
            orientation = orientation
        )
    }

    private fun getPipsForHeading(
        placed: PlacedTile,
        heading: Heading,
        orientation: TileOrientation
    ): Pair<Int, Int> {
        if (placed.isDouble) return placed.tile.a to placed.tile.b
        return if (orientation == TileOrientation.HORIZONTAL) {
            if (heading == Heading.E) placed.innerPip to placed.outerPip
            else placed.outerPip to placed.innerPip
        } else {
            if (heading == Heading.S) placed.innerPip to placed.outerPip
            else placed.outerPip to placed.innerPip
        }
    }

    private fun rectFor(cx: Float, cy: Float, w: Float, h: Float): PlacedRect =
        PlacedRect(cx - w / 2f, cx + w / 2f, cy - h / 2f, cy + h / 2f)

    private fun isAdjacentToAny(rect: PlacedRect, tileRects: List<PlacedRect>, maxGap: Float = 4f): Boolean =
        tileRects.any { other ->
            val overlapX = rect.minX < other.maxX && rect.maxX > other.minX
            val overlapY = rect.minY < other.maxY && rect.maxY > other.minY
            val closeX = rect.minX <= other.maxX + maxGap && rect.maxX + maxGap >= other.minX
            val closeY = rect.minY <= other.maxY + maxGap && rect.maxY + maxGap >= other.minY
            (overlapX && closeY) || (overlapY && closeX)
        }

    private fun offsetFor(
        attachedPos: PositionedTile,
        direction: Direction,
        dims: TileDims
    ): Pair<Float, Float> = when (direction) {
        Direction.LEFT -> -(attachedPos.width / 2f + dims.width / 2f + TILE_GAP) to 0f
        Direction.RIGHT -> (attachedPos.width / 2f + dims.width / 2f + TILE_GAP) to 0f
        Direction.UP -> 0f to -(attachedPos.height / 2f + dims.height / 2f + TILE_GAP)
        Direction.DOWN -> 0f to (attachedPos.height / 2f + dims.height / 2f + TILE_GAP)
    }

    private fun computePosition(
        attachedTo: TileId,
        direction: Direction,
        posById: Map<TileId, PositionedTile>,
        isDouble: Boolean
    ): Pair<Float, Float> {
        val attachedPos = posById.getValue(attachedTo)
        val dims = tileDimensions(direction.toHeading(), isDouble)
        val (dx, dy) = offsetFor(attachedPos, direction, dims)
        return (attachedPos.x + dx) to (attachedPos.y + dy)
    }

    fun compute(board: BoardBranches, mode: GameMode): BoardLayoutResult {
        val root = board.root ?: return BoardLayoutResult(
            tiles = emptyList(),
            dropZones = emptyList(),
            hasSpinner = false,
            bounds = BoardBounds(-120f, 120f, -90f, 90f, 240f, 180f, 120f, 90f)
        )

        val positioned = mutableListOf<PositionedTile>()
        val dropZones = mutableListOf<PositionedDropZone>()
        val posById = mutableMapOf<TileId, PositionedTile>()
        val existingTileRects = mutableListOf<PlacedRect>()

        val rootDims = tileDimensions(Heading.E, root.isDouble)
        val rootPips = getPipsForHeading(root, Heading.E, rootDims.orientation)
        val rootPos = PositionedTile(
            placed = root,
            x = 0f,
            y = 0f,
            width = rootDims.width,
            height = rootDims.height,
            orientation = rootDims.orientation,
            firstPip = rootPips.first,
            secondPip = rootPips.second
        )
        positioned.add(rootPos)
        posById[root.tile.id] = rootPos
        existingTileRects.add(rectFor(rootPos.x, rootPos.y, rootPos.width, rootPos.height))

        fun layoutBranch(
            branch: BranchDir,
            tiles: List<PlacedTile>,
            anchorTileId: TileId
        ) {
            var currentAnchorId = anchorTileId
            val defaultDirection = branch.toDirection()
            for (placed in tiles) {
                val attachedTo: TileId = placed.attachedTo ?: currentAnchorId
                val direction: Direction = placed.direction ?: defaultDirection
                val heading = direction.toHeading()
                val dims = tileDimensions(heading, placed.isDouble)
                val (cx, cy) = computePosition(
                    attachedTo = attachedTo,
                    direction = direction,
                    posById = posById,
                    isDouble = placed.isDouble
                )
                val candidateRect = rectFor(cx, cy, dims.width, dims.height)

                // Safety-net adjacency verification only (does not override computed position)
                @Suppress("UNUSED_VARIABLE")
                val isAdjacentVerified = isAdjacentToAny(candidateRect, existingTileRects, maxGap = 4f)

                val pips = getPipsForHeading(placed, heading, dims.orientation)
                val pos = PositionedTile(
                    placed = placed,
                    x = cx,
                    y = cy,
                    width = dims.width,
                    height = dims.height,
                    orientation = dims.orientation,
                    firstPip = pips.first,
                    secondPip = pips.second
                )
                positioned.add(pos)
                posById[placed.tile.id] = pos
                existingTileRects.add(candidateRect)
                currentAnchorId = placed.tile.id
            }
        }

        layoutBranch(BranchDir.LEFT, board.left, root.tile.id)
        layoutBranch(BranchDir.RIGHT, board.right, root.tile.id)

        val spinnerPlaced = DominoEngine.findSpinnerTile(board)
        val spinnerPos = spinnerPlaced?.let { posById[it.tile.id] } ?: rootPos

        layoutBranch(BranchDir.UP, board.up, spinnerPos.placed.tile.id)
        layoutBranch(BranchDir.DOWN, board.down, spinnerPos.placed.tile.id)

        val hasSpinner = spinnerPlaced != null && posById.containsKey(spinnerPlaced.tile.id)
        val anchorX = if (hasSpinner) spinnerPos.x else 0f
        val anchorY = if (hasSpinner) spinnerPos.y else 0f

        val openEnds = DominoEngine.getOpenEnds(board, mode)
        for (end in openEnds) {
            val attachedTo: TileId = end.anchorTileId
            val direction: Direction = end.branch.toDirection()
            if (!posById.containsKey(attachedTo)) continue

            val heading = direction.toHeading()
            val dims = tileDimensions(heading, false)
            val (dzX, dzY) = computePosition(
                attachedTo = attachedTo,
                direction = direction,
                posById = posById,
                isDouble = false
            )
            val dzRect = rectFor(dzX, dzY, dims.width, dims.height)

            // Safety-net adjacency verification only (does not override computed position)
            @Suppress("UNUSED_VARIABLE")
            val isDzAdjacentVerified = isAdjacentToAny(dzRect, existingTileRects, maxGap = 4f)

            dropZones.add(
                PositionedDropZone(
                    end = end,
                    x = dzX,
                    y = dzY,
                    width = dims.width,
                    height = dims.height,
                    heading = heading
                )
            )
        }

        // Pin active spinner at (0, 0) so it stays centered on screen
        if (anchorX != 0f || anchorY != 0f) {
            positioned.forEach {
                it.x -= anchorX
                it.y -= anchorY
            }
            dropZones.forEach {
                it.x -= anchorX
                it.y -= anchorY
            }
        }

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        positioned.forEach { t ->
            minX = min(minX, t.x - t.width / 2f)
            maxX = max(maxX, t.x + t.width / 2f)
            minY = min(minY, t.y - t.height / 2f)
            maxY = max(maxY, t.y + t.height / 2f)
        }
        dropZones.forEach { dz ->
            minX = min(minX, dz.x - dz.width / 2f)
            maxX = max(maxX, dz.x + dz.width / 2f)
            minY = min(minY, dz.y - dz.height / 2f)
            maxY = max(maxY, dz.y + dz.height / 2f)
        }

        val padding = 32f
        minX -= padding
        maxX += padding
        minY -= padding
        maxY += padding

        val halfSpanX = max(120f, max(abs(minX), abs(maxX)))
        val halfSpanY = max(90f, max(abs(minY), abs(maxY)))

        return BoardLayoutResult(
            tiles = positioned,
            dropZones = dropZones,
            hasSpinner = hasSpinner,
            bounds = BoardBounds(
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                width = max(240f, maxX - minX),
                height = max(180f, maxY - minY),
                halfSpanX = halfSpanX,
                halfSpanY = halfSpanY
            )
        )
    }
}
