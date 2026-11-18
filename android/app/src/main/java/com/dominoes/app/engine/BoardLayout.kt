package com.dominoes.app.engine

import com.dominoes.app.model.BoardBranches
import com.dominoes.app.model.BranchDir
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.OpenEnd
import com.dominoes.app.model.PlacedTile
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
    val end: OpenEnd,
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

    private data class CursorState(
        val x: Float,
        val y: Float,
        val heading: Heading,
        val lastTile: PositionedTile
    )

    private data class AdvanceResult(
        val cx: Float,
        val cy: Float,
        val heading: Heading
    )

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

    private fun advanceCursor(
        cursor: CursorState,
        branch: BranchDir,
        nextTile: PlacedTile?
    ): AdvanceResult {
        val lastTile = cursor.lastTile
        var heading = cursor.heading
        val canTurn = !lastTile.placed.isDouble

        if (canTurn) {
            when (branch) {
                BranchDir.RIGHT -> {
                    if (heading == Heading.E && cursor.x >= 210f) heading = Heading.N
                    else if (heading == Heading.N && cursor.y <= -165f) heading = Heading.E
                }
                BranchDir.LEFT -> {
                    if (heading == Heading.W && cursor.x <= -210f) heading = Heading.S
                    else if (heading == Heading.S && cursor.y >= 165f) heading = Heading.W
                }
                BranchDir.UP -> {
                    if (heading == Heading.N && cursor.y <= -155f) heading = Heading.W
                    else if (heading == Heading.W && cursor.x <= -340f) heading = Heading.N
                }
                BranchDir.DOWN -> {
                    if (heading == Heading.S && cursor.y >= 155f) heading = Heading.E
                    else if (heading == Heading.E && cursor.x >= 340f) heading = Heading.S
                }
            }
        }

        val isDouble = nextTile?.isDouble ?: false
        val dims = tileDimensions(heading, isDouble)

        if (heading != cursor.heading) {
            val prev = cursor.heading
            var anchorX = lastTile.x
            var anchorY = lastTile.y
            when {
                prev == Heading.E && heading == Heading.N -> {
                    anchorX = lastTile.x + lastTile.width / 2f - TILE_SHORT / 2f
                    anchorY = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
                }
                prev == Heading.N && heading == Heading.E -> {
                    anchorX = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
                    anchorY = lastTile.y - lastTile.height / 2f + TILE_SHORT / 2f
                }
                prev == Heading.W && heading == Heading.S -> {
                    anchorX = lastTile.x - lastTile.width / 2f + TILE_SHORT / 2f
                    anchorY = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
                }
                prev == Heading.S && heading == Heading.W -> {
                    anchorX = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
                    anchorY = lastTile.y + lastTile.height / 2f - TILE_SHORT / 2f
                }
                prev == Heading.N && heading == Heading.W -> {
                    anchorX = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
                    anchorY = lastTile.y - lastTile.height / 2f + TILE_SHORT / 2f
                }
                prev == Heading.W && heading == Heading.N -> {
                    anchorX = lastTile.x - lastTile.width / 2f + TILE_SHORT / 2f
                    anchorY = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
                }
                prev == Heading.S && heading == Heading.E -> {
                    anchorX = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
                    anchorY = lastTile.y + lastTile.height / 2f - TILE_SHORT / 2f
                }
                prev == Heading.E && heading == Heading.S -> {
                    anchorX = lastTile.x + lastTile.width / 2f - TILE_SHORT / 2f
                    anchorY = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
                }
            }
            return AdvanceResult(anchorX, anchorY, heading)
        }

        var cx = lastTile.x
        var cy = lastTile.y
        when (heading) {
            Heading.E -> cx = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
            Heading.W -> cx = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
            Heading.N -> cy = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
            Heading.S -> cy = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
        }
        return AdvanceResult(cx, cy, heading)
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
        val posById = mutableMapOf<String, PositionedTile>()

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

        val armCursors = mutableMapOf<BranchDir, CursorState>()

        listOf(BranchDir.RIGHT to board.right, BranchDir.LEFT to board.left).forEach { (branch, list) ->
            val initialHeading = if (branch == BranchDir.RIGHT) Heading.E else Heading.W
            var cursor = CursorState(rootPos.x, rootPos.y, initialHeading, rootPos)
            for (placed in list) {
                val adv = advanceCursor(cursor, branch, placed)
                val dims = tileDimensions(adv.heading, placed.isDouble)
                val pips = getPipsForHeading(placed, adv.heading, dims.orientation)
                val pos = PositionedTile(
                    placed = placed,
                    x = adv.cx,
                    y = adv.cy,
                    width = dims.width,
                    height = dims.height,
                    orientation = dims.orientation,
                    firstPip = pips.first,
                    secondPip = pips.second
                )
                positioned.add(pos)
                posById[placed.tile.id] = pos
                cursor = CursorState(adv.cx, adv.cy, adv.heading, pos)
            }
            armCursors[branch] = cursor
        }

        val spinnerPlaced = DominoEngine.findSpinnerTile(board)
        val spinnerPos = spinnerPlaced?.let { posById[it.tile.id] } ?: rootPos

        listOf(BranchDir.UP to board.up, BranchDir.DOWN to board.down).forEach { (branch, list) ->
            val initialHeading = if (branch == BranchDir.UP) Heading.N else Heading.S
            var cursor = CursorState(spinnerPos.x, spinnerPos.y, initialHeading, spinnerPos)
            for (placed in list) {
                val adv = advanceCursor(cursor, branch, placed)
                val dims = tileDimensions(adv.heading, placed.isDouble)
                val pips = getPipsForHeading(placed, adv.heading, dims.orientation)
                val pos = PositionedTile(
                    placed = placed,
                    x = adv.cx,
                    y = adv.cy,
                    width = dims.width,
                    height = dims.height,
                    orientation = dims.orientation,
                    firstPip = pips.first,
                    secondPip = pips.second
                )
                positioned.add(pos)
                posById[placed.tile.id] = pos
                cursor = CursorState(adv.cx, adv.cy, adv.heading, pos)
            }
            armCursors[branch] = cursor
        }

        val openEnds = DominoEngine.getOpenEnds(board, mode)
        for (end in openEnds) {
            val cursor = armCursors[end.branch] ?: continue
            val adv = advanceCursor(cursor, end.branch, null)
            val dims = tileDimensions(adv.heading, false)
            dropZones.add(
                PositionedDropZone(
                    end = end,
                    x = adv.cx,
                    y = adv.cy,
                    width = dims.width,
                    height = dims.height,
                    heading = adv.heading
                )
            )
        }

        // Pin active spinner at (0, 0) so it never drifts off-center
        val hasSpinner = spinnerPlaced != null && posById.containsKey(spinnerPlaced.tile.id)
        val anchorX = if (hasSpinner) spinnerPos.x else 0f
        val anchorY = if (hasSpinner) spinnerPos.y else 0f

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
