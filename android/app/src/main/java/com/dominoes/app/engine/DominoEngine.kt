package com.dominoes.app.engine

import com.dominoes.app.model.BoardBranches
import com.dominoes.app.model.BranchDir
import com.dominoes.app.model.Difficulty
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.GamePhase
import com.dominoes.app.model.MatchState
import com.dominoes.app.model.ModeMeta
import com.dominoes.app.model.OpenEnd
import com.dominoes.app.model.PlacedTile
import com.dominoes.app.model.PlayTarget
import com.dominoes.app.model.Player
import com.dominoes.app.model.PlayerRoundBreakdown
import com.dominoes.app.model.RoundSummary
import com.dominoes.app.model.ScoringEvent
import com.dominoes.app.model.WinType
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

data class LegalMove(
    val tile: DominoTile,
    val target: PlayTarget
)

sealed interface AIDecision {
    data class Play(val move: LegalMove) : AIDecision
    data object Draw : AIDecision
    data object Pass : AIDecision
}

data class PlayScoreResult(
    val points: Int,
    val reason: String
)

object DominoEngine {

    val AI_NAMES = listOf("Maya", "Theo", "Nadia", "Liam")

    val ROUND_TARGET_OPTIONS = listOf(1, 3, 5, 7)
    val POINT_TARGET_OPTIONS = listOf(50, 100, 150, 200)

    val MODE_META: Map<GameMode, ModeMeta> = mapOf(
        GameMode.FREE_GAME to ModeMeta(
            id = GameMode.FREE_GAME,
            name = "Free Game",
            subtitle = "Open dominoes · Draw & empty hand",
            defaultTarget = 3,
            hasBoneyard = true,
            hasSpinner = false,
            shortRule = "7 tiles each. Draw from boneyard when stuck. First to empty hand or lowest pips when blocked wins 1 round."
        ),
        GameMode.BLOCK to ModeMeta(
            id = GameMode.BLOCK,
            name = "Block",
            subtitle = "Classic tactical dominoes",
            defaultTarget = 3,
            hasBoneyard = false,
            hasSpinner = false,
            shortRule = "No drawing. Match open ends or pass. First to empty hand or lowest pips when blocked wins 1 round."
        ),
        GameMode.ALL_FIVES to ModeMeta(
            id = GameMode.ALL_FIVES,
            name = "All Fives",
            subtitle = "Muggins · Multiples of 5",
            defaultTarget = 100,
            hasBoneyard = true,
            hasSpinner = true,
            shortRule = "Draw when stuck. Score the sum whenever open ends total a multiple of 5. First double is a 4-way spinner."
        ),
        GameMode.ALL_THREES to ModeMeta(
            id = GameMode.ALL_THREES,
            name = "All Threes",
            subtitle = "Fast scoring · Multiples of 3",
            defaultTarget = 100,
            hasBoneyard = true,
            hasSpinner = true,
            shortRule = "Draw when stuck. Score the sum whenever open ends total a multiple of 3. First double is a 4-way spinner."
        ),
        GameMode.FIVES_AND_THREES to ModeMeta(
            id = GameMode.FIVES_AND_THREES,
            name = "Fives & Threes",
            subtitle = "1 pt per 5 or 3",
            defaultTarget = 100,
            hasBoneyard = true,
            hasSpinner = true,
            shortRule = "Score 1 point per 5 and 1 point per 3 whenever open ends divide by 5 or 3 (e.g. 15 = 8 pts)."
        )
    )

    fun metaFor(mode: GameMode): ModeMeta = MODE_META.getValue(mode)

    fun isRoundBasedMode(mode: GameMode): Boolean =
        mode == GameMode.FREE_GAME || mode == GameMode.BLOCK

    fun createDoubleSixSet(): List<DominoTile> {
        val deck = mutableListOf<DominoTile>()
        for (a in 0..6) {
            for (b in a..6) {
                deck.add(DominoTile(id = "$a-$b", a = a, b = b))
            }
        }
        return deck
    }

    fun sumPips(hand: List<DominoTile>): Int = hand.sumOf { it.a + it.b }

    private fun getHandSize(mode: GameMode, playerCount: Int): Int {
        if (mode == GameMode.BLOCK || mode == GameMode.FREE_GAME) return 7
        return when (playerCount) {
            2 -> 7
            3 -> 6
            else -> 5
        }
    }

    private fun findStartingPlayerIndex(players: List<Player>): Int {
        for (pip in 6 downTo 0) {
            for (i in players.indices) {
                if (players[i].hand.any { it.a == pip && it.b == pip }) {
                    return i
                }
            }
        }
        var bestIdx = 0
        var bestVal = -1
        players.forEachIndexed { idx, p ->
            p.hand.forEach { t ->
                if (t.a + t.b > bestVal) {
                    bestVal = t.a + t.b
                    bestIdx = idx
                }
            }
        }
        return bestIdx
    }

    fun createNewMatch(
        mode: GameMode,
        playerCount: Int,
        passAndPlay: Boolean,
        targetScore: Int,
        difficulty: Difficulty
    ): MatchState {
        val players = (0 until playerCount).map { i ->
            val isHuman = i == 0 || (passAndPlay && playerCount == 4 && i == 2)
            val name = if (isHuman) {
                if (i == 0) "You" else "Player 3"
            } else {
                AI_NAMES.getOrElse(i - 1) { "Bot $i" }
            }
            Player(
                id = i,
                name = name,
                isAI = !isHuman,
                avatarIndex = i,
                hand = emptyList(),
                score = 0,
                roundsWon = 0
            )
        }

        val initial = MatchState(
            phase = GamePhase.PLAYING,
            mode = mode,
            playerCount = playerCount,
            passAndPlay = passAndPlay,
            targetScore = targetScore,
            difficulty = difficulty,
            roundNumber = 1,
            players = players,
            currentPlayerIndex = 0,
            boneyard = emptyList(),
            board = BoardBranches(),
            consecutivePasses = 0,
            moveSequence = 0
        )

        return dealRound(initial, roundStarterId = null)
    }

    fun dealRound(prevState: MatchState, roundStarterId: Int?): MatchState {
        val deck = createDoubleSixSet().shuffled()
        val handSize = getHandSize(prevState.mode, prevState.playerCount)
        var cursor = 0

        val players = prevState.players.map { p ->
            val hand = deck.subList(cursor, (cursor + handSize).coerceAtMost(deck.size)).toList()
            cursor += hand.size
            p.copy(hand = hand)
        }

        val boneyard = if (cursor < deck.size) deck.subList(cursor, deck.size).toList() else emptyList()
        val starterIdx = if (roundStarterId != null) {
            max(0, players.indexOfFirst { it.id == roundStarterId })
        } else {
            findStartingPlayerIndex(players)
        }

        val starter = players[starterIdx]
        val needsPassScreen = prevState.passAndPlay && !starter.isAI && prevState.roundNumber > 1

        return prevState.copy(
            phase = if (needsPassScreen) GamePhase.PASS_DEVICE else GamePhase.PLAYING,
            players = players,
            boneyard = boneyard,
            board = BoardBranches(),
            currentPlayerIndex = starterIdx,
            consecutivePasses = 0,
            moveSequence = 0,
            lastPlayedTileId = null,
            lastDrawnTileId = null,
            lastDrawnPlayerId = null,
            lastScoringEvent = null,
            roundSummary = null,
            actionBanner = if (prevState.roundNumber == 1) {
                "${starter.name} opens Round 1"
            } else {
                "${starter.name} leads Round ${prevState.roundNumber}"
            }
        )
    }

    fun findSpinnerTile(board: BoardBranches): PlacedTile? {
        val spinnerId = board.spinnerTileId ?: return null
        if (board.root?.tile?.id == spinnerId) return board.root
        board.left.find { it.tile.id == spinnerId }?.let { return it }
        board.right.find { it.tile.id == spinnerId }?.let { return it }
        return null
    }

    fun isSpinnerCrossed(board: BoardBranches): Boolean {
        val spinnerId = board.spinnerTileId ?: return false
        return when (board.spinnerLocation) {
            PlayTarget.ROOT -> board.left.isNotEmpty() && board.right.isNotEmpty()
            PlayTarget.LEFT -> {
                val idx = board.left.indexOfFirst { it.tile.id == spinnerId }
                idx >= 0 && board.left.size - 1 > idx
            }
            PlayTarget.RIGHT -> {
                val idx = board.right.indexOfFirst { it.tile.id == spinnerId }
                idx >= 0 && board.right.size - 1 > idx
            }
            else -> false
        }
    }

    fun getOpenEnds(board: BoardBranches, mode: GameMode): List<OpenEnd> {
        val root = board.root ?: return emptyList()
        val ends = mutableListOf<OpenEnd>()
        val singleTileOnly = board.left.isEmpty() &&
            board.right.isEmpty() &&
            board.up.isEmpty() &&
            board.down.isEmpty()

        if (singleTileOnly) {
            ends.add(
                OpenEnd(
                    branch = BranchDir.LEFT,
                    requiredPip = root.innerPip,
                    scoreValue = root.innerPip,
                    isDoubleTip = root.isDouble,
                    anchorTileId = root.tile.id
                )
            )
            ends.add(
                OpenEnd(
                    branch = BranchDir.RIGHT,
                    requiredPip = root.outerPip,
                    scoreValue = root.outerPip,
                    isDoubleTip = root.isDouble,
                    anchorTileId = root.tile.id
                )
            )
            return ends
        }

        if (board.left.isNotEmpty()) {
            val tip = board.left.last()
            ends.add(
                OpenEnd(
                    branch = BranchDir.LEFT,
                    requiredPip = tip.outerPip,
                    scoreValue = if (tip.isDouble) tip.outerPip * 2 else tip.outerPip,
                    isDoubleTip = tip.isDouble,
                    anchorTileId = tip.tile.id
                )
            )
        } else {
            ends.add(
                OpenEnd(
                    branch = BranchDir.LEFT,
                    requiredPip = root.innerPip,
                    scoreValue = if (root.isDouble) root.innerPip * 2 else root.innerPip,
                    isDoubleTip = root.isDouble,
                    anchorTileId = root.tile.id
                )
            )
        }

        if (board.right.isNotEmpty()) {
            val tip = board.right.last()
            ends.add(
                OpenEnd(
                    branch = BranchDir.RIGHT,
                    requiredPip = tip.outerPip,
                    scoreValue = if (tip.isDouble) tip.outerPip * 2 else tip.outerPip,
                    isDoubleTip = tip.isDouble,
                    anchorTileId = tip.tile.id
                )
            )
        } else {
            ends.add(
                OpenEnd(
                    branch = BranchDir.RIGHT,
                    requiredPip = root.outerPip,
                    scoreValue = if (root.isDouble) root.outerPip * 2 else root.outerPip,
                    isDoubleTip = root.isDouble,
                    anchorTileId = root.tile.id
                )
            )
        }

        if (metaFor(mode).hasSpinner && isSpinnerCrossed(board)) {
            val spinner = findSpinnerTile(board)
            if (spinner != null) {
                if (board.up.isNotEmpty()) {
                    val tip = board.up.last()
                    ends.add(
                        OpenEnd(
                            branch = BranchDir.UP,
                            requiredPip = tip.outerPip,
                            scoreValue = if (tip.isDouble) tip.outerPip * 2 else tip.outerPip,
                            isDoubleTip = tip.isDouble,
                            anchorTileId = tip.tile.id
                        )
                    )
                } else {
                    ends.add(
                        OpenEnd(
                            branch = BranchDir.UP,
                            requiredPip = spinner.tile.a,
                            scoreValue = 0,
                            isDoubleTip = false,
                            anchorTileId = spinner.tile.id
                        )
                    )
                }

                if (board.down.isNotEmpty()) {
                    val tip = board.down.last()
                    ends.add(
                        OpenEnd(
                            branch = BranchDir.DOWN,
                            requiredPip = tip.outerPip,
                            scoreValue = if (tip.isDouble) tip.outerPip * 2 else tip.outerPip,
                            isDoubleTip = tip.isDouble,
                            anchorTileId = tip.tile.id
                        )
                    )
                } else {
                    ends.add(
                        OpenEnd(
                            branch = BranchDir.DOWN,
                            requiredPip = spinner.tile.a,
                            scoreValue = 0,
                            isDoubleTip = false,
                            anchorTileId = spinner.tile.id
                        )
                    )
                }
            }
        }

        return ends
    }

    fun getOpenEndsSum(board: BoardBranches, mode: GameMode): Int {
        if (board.root == null) return 0
        return getOpenEnds(board, mode).sumOf { it.scoreValue }
    }

    fun calculatePlayScore(openSum: Int, mode: GameMode): PlayScoreResult {
        if (openSum <= 0) return PlayScoreResult(0, "")
        return when (mode) {
            GameMode.ALL_FIVES -> if (openSum % 5 == 0) {
                PlayScoreResult(openSum, "Multiple of 5 ($openSum)")
            } else {
                PlayScoreResult(0, "")
            }
            GameMode.ALL_THREES -> if (openSum % 3 == 0) {
                PlayScoreResult(openSum, "Multiple of 3 ($openSum)")
            } else {
                PlayScoreResult(0, "")
            }
            GameMode.FIVES_AND_THREES -> {
                val fives = if (openSum % 5 == 0) openSum / 5 else 0
                val threes = if (openSum % 3 == 0) openSum / 3 else 0
                val points = fives + threes
                if (points > 0) {
                    val parts = buildList {
                        if (fives > 0) add("${fives}×5")
                        if (threes > 0) add("${threes}×3")
                    }
                    PlayScoreResult(points, "Open $openSum (${parts.joinToString(" + ")})")
                } else {
                    PlayScoreResult(0, "")
                }
            }
            GameMode.FREE_GAME, GameMode.BLOCK -> PlayScoreResult(0, "")
        }
    }

    fun getLegalMoves(hand: List<DominoTile>, board: BoardBranches, mode: GameMode): List<LegalMove> {
        if (board.root == null) {
            return hand.map { LegalMove(it, PlayTarget.ROOT) }
        }
        val openEnds = getOpenEnds(board, mode)
        val moves = mutableListOf<LegalMove>()
        for (tile in hand) {
            for (end in openEnds) {
                if (tileCanMatchEnd(tile, end)) {
                    moves.add(LegalMove(tile, PlayTarget.fromBranch(end.branch)))
                }
            }
        }
        return moves
    }

    fun tileCanMatchEnd(tile: DominoTile, end: OpenEnd): Boolean =
        tile.a == end.requiredPip || tile.b == end.requiredPip

    private fun placeTileOnBoard(
        board: BoardBranches,
        tile: DominoTile,
        target: PlayTarget,
        playerId: Int,
        sequence: Int,
        mode: GameMode
    ): BoardBranches {
        val isDouble = tile.a == tile.b
        val canBecomeSpinner = metaFor(mode).hasSpinner && isDouble && board.spinnerTileId == null

        if (target == PlayTarget.ROOT || board.root == null) {
            val placed = PlacedTile(
                tile = tile,
                target = PlayTarget.ROOT,
                innerPip = tile.a,
                outerPip = tile.b,
                isDouble = isDouble,
                isSpinner = canBecomeSpinner,
                playedBy = playerId,
                sequence = sequence
            )
            return board.copy(
                root = placed,
                spinnerTileId = if (canBecomeSpinner) tile.id else board.spinnerTileId,
                spinnerLocation = if (canBecomeSpinner) PlayTarget.ROOT else board.spinnerLocation
            )
        }

        val branchDir = target.toBranchDirOrNull() ?: return board
        val openEnds = getOpenEnds(board, mode)
        val targetEnd = openEnds.find { it.branch == branchDir } ?: return board

        val req = targetEnd.requiredPip
        val innerPip = req
        val outerPip = if (tile.a == req) tile.b else tile.a

        val placed = PlacedTile(
            tile = tile,
            target = target,
            innerPip = innerPip,
            outerPip = outerPip,
            isDouble = isDouble,
            isSpinner = canBecomeSpinner,
            playedBy = playerId,
            sequence = sequence
        )

        val newSpinnerLoc = if (canBecomeSpinner && (target == PlayTarget.LEFT || target == PlayTarget.RIGHT)) {
            target
        } else {
            board.spinnerLocation
        }

        return when (branchDir) {
            BranchDir.LEFT -> board.copy(
                left = board.left + placed,
                spinnerTileId = if (canBecomeSpinner) tile.id else board.spinnerTileId,
                spinnerLocation = newSpinnerLoc
            )
            BranchDir.RIGHT -> board.copy(
                right = board.right + placed,
                spinnerTileId = if (canBecomeSpinner) tile.id else board.spinnerTileId,
                spinnerLocation = newSpinnerLoc
            )
            BranchDir.UP -> board.copy(
                up = board.up + placed,
                spinnerTileId = if (canBecomeSpinner) tile.id else board.spinnerTileId,
                spinnerLocation = newSpinnerLoc
            )
            BranchDir.DOWN -> board.copy(
                down = board.down + placed,
                spinnerTileId = if (canBecomeSpinner) tile.id else board.spinnerTileId,
                spinnerLocation = newSpinnerLoc
            )
        }
    }

    private fun finalizeRound(
        state: MatchState,
        winType: WinType,
        dominoWinnerId: Int?
    ): MatchState {
        val mutablePlayers = state.players.toMutableList()
        var winnerId: Int? = dominoWinnerId
        var finalWinType = winType

        if (winType == WinType.BLOCKED) {
            val counts = mutablePlayers.map { it.id to sumPips(it.hand) }
            val minPips = counts.minOf { it.second }
            val tied = counts.filter { it.second == minPips }
            if (tied.size == 1) {
                winnerId = tied.first().first
            } else {
                winnerId = null
                finalWinType = WinType.TIE
            }
        }

        var pointsAwarded = 0
        val roundBased = isRoundBasedMode(state.mode)
        if (winnerId != null) {
            val idx = mutablePlayers.indexOfFirst { it.id == winnerId }
            val winner = mutablePlayers[idx]
            val opponentsPips = mutablePlayers.filter { it.id != winnerId }.sumOf { sumPips(it.hand) }
            val winnerPips = sumPips(winner.hand)
            val rawDiff = max(0, opponentsPips - winnerPips)

            pointsAwarded = when {
                roundBased -> 0
                state.mode == GameMode.ALL_FIVES -> (rawDiff / 5.0).roundToInt() * 5
                state.mode == GameMode.ALL_THREES -> (rawDiff / 3.0).roundToInt() * 3
                state.mode == GameMode.FIVES_AND_THREES ->
                    (if (winType == WinType.DOMINO) 1 else 0) + floor(rawDiff / 5.0).toInt()
                else -> 0
            }

            mutablePlayers[idx] = winner.copy(
                score = winner.score + pointsAwarded,
                roundsWon = winner.roundsWon + 1
            )
        }

        val eligible = if (roundBased) {
            mutablePlayers.filter { it.roundsWon >= state.targetScore }
        } else {
            mutablePlayers.filter { it.score >= state.targetScore }
        }

        val matchWinnerId = if (eligible.isNotEmpty()) {
            eligible.sortedByDescending { if (roundBased) it.roundsWon else it.score }.first().id
        } else {
            null
        }

        val summary = RoundSummary(
            roundNumber = state.roundNumber,
            winnerId = winnerId,
            winnerName = winnerId?.let { id -> mutablePlayers.first { it.id == id }.name } ?: "Draw",
            winType = finalWinType,
            pointsAwarded = pointsAwarded,
            playerBreakdowns = mutablePlayers.map { p ->
                PlayerRoundBreakdown(
                    playerId = p.id,
                    name = p.name,
                    remainingPips = sumPips(p.hand),
                    remainingTiles = p.hand,
                    totalScore = p.score
                )
            },
            matchWinnerId = matchWinnerId
        )

        return state.copy(
            phase = if (matchWinnerId != null) GamePhase.GAME_OVER else GamePhase.ROUND_SUMMARY,
            players = mutablePlayers,
            roundSummary = summary,
            matchWinnerId = matchWinnerId
        )
    }

    private fun advanceTurn(state: MatchState, banner: String?): MatchState {
        val nextIndex = (state.currentPlayerIndex + 1) % state.players.size
        val nextPlayer = state.players[nextIndex]
        val needsPassScreen = state.passAndPlay &&
            !nextPlayer.isAI &&
            state.players.count { !it.isAI } > 1

        return state.copy(
            phase = if (needsPassScreen) GamePhase.PASS_DEVICE else GamePhase.PLAYING,
            currentPlayerIndex = nextIndex,
            actionBanner = banner
        )
    }

    fun applyMove(state: MatchState, tileId: String, target: PlayTarget): MatchState {
        val currentPlayer = state.players[state.currentPlayerIndex]
        val tile = currentPlayer.hand.find { it.id == tileId } ?: return state

        val nextSeq = state.moveSequence + 1
        val nextBoard = placeTileOnBoard(
            board = state.board,
            tile = tile,
            target = target,
            playerId = currentPlayer.id,
            sequence = nextSeq,
            mode = state.mode
        )

        val nextHand = currentPlayer.hand.filterNot { it.id == tileId }
        val openSum = getOpenEndsSum(nextBoard, state.mode)
        val scoreResult = calculatePlayScore(openSum, state.mode)

        val players = state.players.mapIndexed { idx, p ->
            if (idx == state.currentPlayerIndex) {
                p.copy(hand = nextHand, score = p.score + scoreResult.points)
            } else {
                p
            }
        }

        val updatedPlayer = players[state.currentPlayerIndex]
        val scoringEvent = if (scoreResult.points > 0) {
            ScoringEvent(
                id = nextSeq,
                playerId = currentPlayer.id,
                playerName = currentPlayer.name,
                points = scoreResult.points,
                openSum = openSum,
                reason = scoreResult.reason
            )
        } else {
            null
        }

        val baseState = state.copy(
            players = players,
            board = nextBoard,
            consecutivePasses = 0,
            moveSequence = nextSeq,
            lastPlayedTileId = tile.id,
            lastDrawnTileId = null,
            lastDrawnPlayerId = null,
            lastScoringEvent = scoringEvent
        )

        if (!isRoundBasedMode(state.mode) && updatedPlayer.score >= state.targetScore) {
            val summary = RoundSummary(
                roundNumber = state.roundNumber,
                winnerId = updatedPlayer.id,
                winnerName = updatedPlayer.name,
                winType = WinType.DOMINO,
                pointsAwarded = scoreResult.points,
                playerBreakdowns = players.map { p ->
                    PlayerRoundBreakdown(
                        playerId = p.id,
                        name = p.name,
                        remainingPips = sumPips(p.hand),
                        remainingTiles = p.hand,
                        totalScore = p.score
                    )
                },
                matchWinnerId = updatedPlayer.id
            )
            return baseState.copy(
                phase = GamePhase.GAME_OVER,
                matchWinnerId = updatedPlayer.id,
                roundSummary = summary,
                actionBanner = "${updatedPlayer.name} reached ${updatedPlayer.score} pts and won!"
            )
        }

        if (nextHand.isEmpty()) {
            return finalizeRound(baseState, WinType.DOMINO, updatedPlayer.id)
        }

        val banner = if (scoreResult.points > 0) {
            "${currentPlayer.name} played ${tile.a}|${tile.b} (+${scoreResult.points} pts)"
        } else {
            "${currentPlayer.name} played ${tile.a}|${tile.b}"
        }

        return advanceTurn(baseState, banner)
    }

    fun drawTileFromBoneyard(state: MatchState): MatchState {
        if (!metaFor(state.mode).hasBoneyard || state.boneyard.isEmpty()) return state

        val drawn = state.boneyard.first()
        val remaining = state.boneyard.drop(1)
        val currentPlayer = state.players[state.currentPlayerIndex]

        val players = state.players.mapIndexed { idx, p ->
            if (idx == state.currentPlayerIndex) p.copy(hand = p.hand + drawn) else p
        }

        return state.copy(
            players = players,
            boneyard = remaining,
            lastDrawnTileId = drawn.id,
            lastDrawnPlayerId = currentPlayer.id,
            actionBanner = "${currentPlayer.name} drew from boneyard"
        )
    }

    fun passCurrentTurn(state: MatchState): MatchState {
        val currentPlayer = state.players[state.currentPlayerIndex]
        val nextPasses = state.consecutivePasses + 1

        if (nextPasses >= state.players.size) {
            return finalizeRound(
                state = state.copy(
                    consecutivePasses = nextPasses,
                    lastDrawnTileId = null,
                    lastDrawnPlayerId = null
                ),
                winType = WinType.BLOCKED,
                dominoWinnerId = null
            )
        }

        return advanceTurn(
            state = state.copy(
                consecutivePasses = nextPasses,
                lastDrawnTileId = null,
                lastDrawnPlayerId = null
            ),
            banner = "${currentPlayer.name} passed"
        )
    }

    fun chooseAIMove(state: MatchState): AIDecision {
        val currentPlayer = state.players[state.currentPlayerIndex]
        val legal = getLegalMoves(currentPlayer.hand, state.board, state.mode)

        if (legal.isEmpty()) {
            return if (metaFor(state.mode).hasBoneyard && state.boneyard.isNotEmpty()) {
                AIDecision.Draw
            } else {
                AIDecision.Pass
            }
        }

        if (state.difficulty == Difficulty.EASY) {
            return AIDecision.Play(legal[Random.nextInt(legal.size)])
        }

        data class ScoredMove(val move: LegalMove, val utility: Double)

        val scored = legal.map { move ->
            val simBoard = placeTileOnBoard(
                board = state.board,
                tile = move.tile,
                target = move.target,
                playerId = currentPlayer.id,
                sequence = state.moveSequence + 1,
                mode = state.mode
            )
            val openSum = getOpenEndsSum(simBoard, state.mode)
            val scoreRes = calculatePlayScore(openSum, state.mode)
            val pipWeight = (move.tile.a + move.tile.b).toDouble()
            val isDoubleBonus = if (move.tile.a == move.tile.b) 3.5 else 0.0

            var flexibility = 0.0
            if (state.difficulty == Difficulty.HARD) {
                if (currentPlayer.score + scoreRes.points >= state.targetScore) {
                    return@map ScoredMove(move, 10000.0 + scoreRes.points)
                }
                val remaining = currentPlayer.hand.filterNot { it.id == move.tile.id }
                val nextEnds = getOpenEnds(simBoard, state.mode)
                val matchingCount = remaining.count { t ->
                    nextEnds.any { e -> tileCanMatchEnd(t, e) }
                }
                flexibility = matchingCount * 2.5
            }

            val utility = scoreRes.points * 12.0 + pipWeight * 0.8 + isDoubleBonus + flexibility
            ScoredMove(move, utility)
        }.sortedByDescending { it.utility }

        if (state.difficulty == Difficulty.MEDIUM && scored.size > 1 && Random.nextFloat() < 0.25f) {
            return AIDecision.Play(scored[1].move)
        }

        return AIDecision.Play(scored.first().move)
    }
}
