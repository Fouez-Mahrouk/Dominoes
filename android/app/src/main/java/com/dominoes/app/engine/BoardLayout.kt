package com.dominoes.app.engine

import com.dominoes.app.model.BoardBranches
import com.dominoes.app.model.BranchDir
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.OpenEnd
import com.dominoes.app.model.PlacedTile
import com.dominoes.app.model.PlayTarget
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

    private data class PlacedRect(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    )

    private fun rectFor(cx: Float, cy: Float, w: Float, h: Float): PlacedRect =
        PlacedRect(cx - w / 2f, cx + w / 2f, cy - h / 2f, cy + h / 2f)

    private fun overlapsAny(rect: PlacedRect, rects: List<PlacedRect>, margin: Float = 2f): Boolean =
        rects.any { other ->
            rect.minX < other.maxX + margin &&
                rect.maxX + margin > other.minX &&
                rect.minY < other.maxY + margin &&
                rect.maxY + margin > other.minY
        }

    private fun turnCCW(heading: Heading): Heading = when (heading) {
        Heading.E -> Heading.N
        Heading.N -> Heading.W
        Heading.W -> Heading.S
        Heading.S -> Heading.E
    }

    private fun turnCW(heading: Heading): Heading = when (heading) {
        Heading.E -> Heading.S
        Heading.S -> Heading.W
        Heading.W -> Heading.N
        Heading.N -> Heading.E
    }

    private fun computeCandidate(
        lastTile: PositionedTile,
        prevHeading: Heading,
        newHeading: Heading,
        nextTile: PlacedTile?
    ): AdvanceResult {
        val isDouble = nextTile?.isDouble ?: false
        val dims = tileDimensions(newHeading, isDouble)

        if (newHeading != prevHeading) {
            var anchorX = lastTile.x
            var anchorY = lastTile.y
            when {
                prevHeading == Heading.E && newHeading == Heading.N -> {
                    anchorX = lastTile.x + lastTile.width / 2f - TILE_SHORT / 2f
                    anchorY = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
                }
                prevHeading == Heading.N && newHeading == Heading.E -> {
                    anchorX = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
                    anchorY = lastTile.y - lastTile.height / 2f + TILE_SHORT / 2f
                }
                prevHeading == Heading.W && newHeading == Heading.S -> {
                    anchorX = lastTile.x - lastTile.width / 2f + TILE_SHORT / 2f
                    anchorY = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
                }
                prevHeading == Heading.S && newHeading == Heading.W -> {
                    anchorX = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
                    anchorY = lastTile.y + lastTile.height / 2f - TILE_SHORT / 2f
                }
                prevHeading == Heading.N && newHeading == Heading.W -> {
                    anchorX = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
                    anchorY = lastTile.y - lastTile.height / 2f + TILE_SHORT / 2f
                }
                prevHeading == Heading.W && newHeading == Heading.N -> {
                    anchorX = lastTile.x - lastTile.width / 2f + TILE_SHORT / 2f
                    anchorY = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
                }
                prevHeading == Heading.S && newHeading == Heading.E -> {
                    anchorX = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
                    anchorY = lastTile.y + lastTile.height / 2f - TILE_SHORT / 2f
                }
                prevHeading == Heading.E && newHeading == Heading.S -> {
                    anchorX = lastTile.x + lastTile.width / 2f - TILE_SHORT / 2f
                    anchorY = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
                }
            }
            return AdvanceResult(anchorX, anchorY, newHeading)
        }

        var cx = lastTile.x
        var cy = lastTile.y
        when (newHeading) {
            Heading.E -> cx = lastTile.x + lastTile.width / 2f + TILE_GAP + dims.width / 2f
            Heading.W -> cx = lastTile.x - lastTile.width / 2f - TILE_GAP - dims.width / 2f
            Heading.N -> cy = lastTile.y - lastTile.height / 2f - TILE_GAP - dims.height / 2f
            Heading.S -> cy = lastTile.y + lastTile.height / 2f + TILE_GAP + dims.height / 2f
        }
        return AdvanceResult(cx, cy, newHeading)
    }

    private fun advanceCompact(
        cursor: CursorState,
        nextTile: PlacedTile?,
        occupiedRects: List<PlacedRect>,
        centerX: Float,
        centerY: Float
    ): AdvanceResult {
        val lastTile = cursor.lastTile
        val curHeading = cursor.heading
        val isDouble = nextTile?.isDouble ?: false
        val straight = computeCandidate(lastTile, curHeading, curHeading, nextTile)

        val candidates = mutableListOf<Pair<AdvanceResult, Float>>()
        candidates.add(straight to 4f)

        if (!lastTile.placed.isDouble && !isDouble) {
            val ccw = computeCandidate(lastTile, curHeading, turnCCW(curHeading), nextTile)
            val cw = computeCandidate(lastTile, curHeading, turnCW(curHeading), nextTile)
            candidates.add(ccw to 0f)
            candidates.add(cw to 8f)
        }

        val valid = candidates.filter { (cand, _) ->
            val dims = tileDimensions(cand.heading, isDouble)
            !overlapsAny(rectFor(cand.cx, cand.cy, dims.width, dims.height), occupiedRects)
        }

        if (valid.isEmpty()) return straight
        if (valid.size == 1) return valid.first().first

        val curDist = kotlin.math.hypot((lastTile.x - centerX).toDouble(), (lastTile.y - centerY).toDouble()).toFloat()
        val straightValid = valid.find { it.first == straight }?.first
        if (curDist < 145f && straightValid != null) {
            return straightValid
        }

        return valid.minBy { (cand, bias) ->
            val d = kotlin.math.hypot((cand.cx - centerX).toDouble(), (cand.cy - centerY).toDouble()).toFloat()
            d + bias
        }.first
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
        val occupiedRects = mutableListOf<PlacedRect>()

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
        occupiedRects.add(rectFor(rootPos.x, rootPos.y, rootPos.width, rootPos.height))

        val armCursors = mutableMapOf<BranchDir, CursorState>()
        val modeHasSpinner = DominoEngine.metaFor(mode).hasSpinner

        // Reserve open root/spinner entry doorways so longer arms wrap around them without blocking shorter or open arms
        val stubRects = mutableMapOf<BranchDir, PlacedRect>()
        stubRects[BranchDir.LEFT] = rectFor(-(rootPos.width / 2f + TILE_GAP + TILE_LONG / 2f), 0f, TILE_LONG, TILE_SHORT)
        stubRects[BranchDir.RIGHT] = rectFor(rootPos.width / 2f + TILE_GAP + TILE_LONG / 2f, 0f, TILE_LONG, TILE_SHORT)
        if (modeHasSpinner && board.spinnerLocation == PlayTarget.ROOT) {
            stubRects[BranchDir.UP] = rectFor(0f, -(rootPos.height / 2f + TILE_GAP + TILE_LONG / 2f), TILE_SHORT, TILE_LONG)
            stubRects[BranchDir.DOWN] = rectFor(0f, rootPos.height / 2f + TILE_GAP + TILE_LONG / 2f, TILE_SHORT, TILE_LONG)
        }

        fun layoutArm(
            branch: BranchDir,
            tiles: List<PlacedTile>,
            startTile: PositionedTile,
            initialHeading: Heading,
            centerX: Float,
            centerY: Float
        ) {
            var cursor = CursorState(startTile.x, startTile.y, initialHeading, startTile)
            for (placed in tiles) {
                val activeBlockers = buildList {
                    addAll(occupiedRects)
                    stubRects.forEach { (b, r) ->
                        if (b != branch) add(r)
                    }
                }
                val adv = advanceCompact(cursor, placed, activeBlockers, centerX, centerY)
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
                occupiedRects.add(rectFor(pos.x, pos.y, pos.width, pos.height))
                cursor = CursorState(adv.cx, adv.cy, adv.heading, pos)
            }
            armCursors[branch] = cursor
        }

        // Lay out shorter left/right arm first to keep the shorter end closest to center
        val horizArms = listOf(BranchDir.LEFT to board.left, BranchDir.RIGHT to board.right)
            .sortedBy { it.second.size }
        for ((branch, list) in horizArms) {
            val initialHeading = if (branch == BranchDir.RIGHT) Heading.E else Heading.W
            layoutArm(branch, list, rootPos, initialHeading, 0f, 0f)
        }

        val spinnerPlaced = DominoEngine.findSpinnerTile(board)
        val spinnerPos = spinnerPlaced?.let { posById[it.tile.id] } ?: rootPos

        if (modeHasSpinner && spinnerPlaced != null) {
            stubRects[BranchDir.UP] = rectFor(
                spinnerPos.x,
                spinnerPos.y - (spinnerPos.height / 2f + TILE_GAP + TILE_LONG / 2f),
                TILE_SHORT,
                TILE_LONG
            )
            stubRects[BranchDir.DOWN] = rectFor(
                spinnerPos.x,
                spinnerPos.y + (spinnerPos.height / 2f + TILE_GAP + TILE_LONG / 2f),
                TILE_SHORT,
                TILE_LONG
            )
        }

        // Lay out shorter spinner vertical arm first
        val vertArms = listOf(BranchDir.UP to board.up, BranchDir.DOWN to board.down)
            .sortedBy { it.second.size }
        for ((branch, list) in vertArms) {
            val initialHeading = if (branch == BranchDir.UP) Heading.N else Heading.S
            layoutArm(branch, list, spinnerPos, initialHeading, spinnerPos.x, spinnerPos.y)
        }

        val hasSpinner = spinnerPlaced != null && posById.containsKey(spinnerPlaced.tile.id)
        val anchorX = if (hasSpinner) spinnerPos.x else 0f
        val anchorY = if (hasSpinner) spinnerPos.y else 0f

        val openEnds = DominoEngine.getOpenEnds(board, mode)
        for (end in openEnds) {
            val cursor = armCursors[end.branch] ?: continue
            val adv = advanceCompact(cursor, null, occupiedRects, anchorX, anchorY)
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
            occupiedRects.add(rectFor(adv.cx, adv.cy, dims.width, dims.height))
        }

        // Pin active spinner at (0, 0) so it never drifts off-center
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
