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


package com.dominoes.app.model

enum class GameMode(val id: String) {
    FREE_GAME("free_game"),
    BLOCK("block"),
    ALL_FIVES("all_fives"),
    ALL_THREES("all_threes"),
    FIVES_AND_THREES("fives_and_threes");

    companion object {
        fun fromId(id: String): GameMode = entries.find { it.id == id } ?: FREE_GAME
    }
}

enum class Difficulty(val id: String, val label: String) {
    EASY("easy", "Easy"),
    MEDIUM("medium", "Medium"),
    HARD("hard", "Hard");

    companion object {
        fun fromId(id: String): Difficulty = entries.find { it.id == id } ?: MEDIUM
    }
}

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

enum class BranchDir(val id: String) {
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down");

    companion object {
        fun fromId(id: String): BranchDir? = entries.find { it.id == id }
    }
}

typealias TileId = String

enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

enum class PlayTarget {
    ROOT,
    LEFT,
    RIGHT,
    UP,
    DOWN;

    fun toBranchDirOrNull(): BranchDir? = when (this) {
        ROOT -> null
        LEFT -> BranchDir.LEFT
        RIGHT -> BranchDir.RIGHT
        UP -> BranchDir.UP
        DOWN -> BranchDir.DOWN
    }

    companion object {
        fun fromBranch(branch: BranchDir): PlayTarget = when (branch) {
            BranchDir.LEFT -> LEFT
            BranchDir.RIGHT -> RIGHT
            BranchDir.UP -> UP
            BranchDir.DOWN -> DOWN
        }
    }
}

enum class GamePhase {
    PLAYING,
    PASS_DEVICE,
    ROUND_SUMMARY,
    GAME_OVER
}

enum class WinType(val id: String) {
    DOMINO("domino"),
    BLOCKED("blocked"),
    TIE("tie");

    companion object {
        fun fromId(id: String): WinType = entries.find { it.id == id } ?: DOMINO
    }
}

data class ModeMeta(
    val id: GameMode,
    val name: String,
    val subtitle: String,
    val defaultTarget: Int,
    val hasBoneyard: Boolean,
    val hasSpinner: Boolean,
    val shortRule: String
)

data class DominoTile(
    val id: String,
    val a: Int,
    val b: Int
)

data class PlacedTile(
    val tile: DominoTile,
    val target: PlayTarget,
    val innerPip: Int,
    val outerPip: Int,
    val isDouble: Boolean,
    val isSpinner: Boolean,
    val playedBy: Int,
    val sequence: Int,
    val attachedTo: TileId? = null,
    val direction: Direction? = null
)

data class OpenEnd(
    val branch: BranchDir,
    val requiredPip: Int,
    val scoreValue: Int,
    val isDoubleTip: Boolean,
    val anchorTileId: String
)

data class Player(
    val id: Int,
    val name: String,
    val isAI: Boolean,
    val avatarIndex: Int,
    val hand: List<DominoTile>,
    val score: Int = 0,
    val roundsWon: Int = 0
)

data class GameSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val showModeTipOnStart: Boolean = true
)

data class ScoringEvent(
    val id: Int,
    val playerId: Int,
    val playerName: String,
    val points: Int,
    val openSum: Int,
    val reason: String
)

data class PlayerRoundBreakdown(
    val playerId: Int,
    val name: String,
    val remainingPips: Int,
    val remainingTiles: List<DominoTile>,
    val totalScore: Int
)

data class RoundSummary(
    val roundNumber: Int,
    val winnerId: Int?,
    val winnerName: String,
    val winType: WinType,
    val pointsAwarded: Int,
    val playerBreakdowns: List<PlayerRoundBreakdown>,
    val matchWinnerId: Int?
)

data class BoardBranches(
    val root: PlacedTile? = null,
    val left: List<PlacedTile> = emptyList(),
    val right: List<PlacedTile> = emptyList(),
    val up: List<PlacedTile> = emptyList(),
    val down: List<PlacedTile> = emptyList(),
    val spinnerTileId: String? = null,
    val spinnerLocation: PlayTarget? = null
)

data class MatchState(
    val phase: GamePhase,
    val mode: GameMode,
    val playerCount: Int,
    val passAndPlay: Boolean,
    val targetScore: Int,
    val difficulty: Difficulty,
    val roundNumber: Int,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val boneyard: List<DominoTile>,
    val board: BoardBranches,
    val consecutivePasses: Int,
    val moveSequence: Int,
    val lastPlayedTileId: String? = null,
    val lastDrawnTileId: String? = null,
    val lastDrawnPlayerId: Int? = null,
    val lastScoringEvent: ScoringEvent? = null,
    val roundSummary: RoundSummary? = null,
    val matchWinnerId: Int? = null,
    val actionBanner: String? = null
)

data class LifetimeStats(
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val roundsWon: Int = 0,
    val highestScore: Int = 0,
    val totalPointsScored: Int = 0
)

data class RoundWinCelebration(
    val roundNumber: Int,
    val winnerId: Int,
    val winnerName: String
)
