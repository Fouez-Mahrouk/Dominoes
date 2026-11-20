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


package com.dominoes.app.data

import android.content.Context
import com.dominoes.app.model.BoardBranches
import com.dominoes.app.model.Difficulty
import com.dominoes.app.model.DominoTile
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.GamePhase
import com.dominoes.app.model.GameSettings
import com.dominoes.app.model.LifetimeStats
import com.dominoes.app.model.MatchState
import com.dominoes.app.model.PlacedTile
import com.dominoes.app.model.PlayTarget
import com.dominoes.app.model.Player
import com.dominoes.app.model.PlayerRoundBreakdown
import com.dominoes.app.model.RoundSummary
import com.dominoes.app.model.ScoringEvent
import com.dominoes.app.model.ThemeMode
import com.dominoes.app.model.WinType
import org.json.JSONArray
import org.json.JSONObject

class LocalStorage(context: Context) {

    private val prefs = context.getSharedPreferences("dominoes_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETTINGS = "settings_v1"
        private const val KEY_STATS = "stats_v1"
        private const val KEY_MATCH = "active_match_v1"
    }

    fun loadSettings(): GameSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return GameSettings()
        return runCatching {
            val o = JSONObject(raw)
            GameSettings(
                theme = ThemeMode.fromId(o.optString("theme", "system")),
                vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                soundEnabled = o.optBoolean("soundEnabled", true),
                difficulty = Difficulty.fromId(o.optString("difficulty", "medium")),
                showModeTipOnStart = o.optBoolean("showModeTipOnStart", true)
            )
        }.getOrDefault(GameSettings())
    }

    fun saveSettings(settings: GameSettings) {
        val o = JSONObject()
            .put("theme", settings.theme.id)
            .put("vibrationEnabled", settings.vibrationEnabled)
            .put("soundEnabled", settings.soundEnabled)
            .put("difficulty", settings.difficulty.id)
            .put("showModeTipOnStart", settings.showModeTipOnStart)
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    fun loadStats(): LifetimeStats {
        val raw = prefs.getString(KEY_STATS, null) ?: return LifetimeStats()
        return runCatching {
            val o = JSONObject(raw)
            LifetimeStats(
                matchesPlayed = o.optInt("matchesPlayed", 0),
                matchesWon = o.optInt("matchesWon", 0),
                roundsWon = o.optInt("roundsWon", 0),
                highestScore = o.optInt("highestScore", 0),
                totalPointsScored = o.optInt("totalPointsScored", 0)
            )
        }.getOrDefault(LifetimeStats())
    }

    fun saveStats(stats: LifetimeStats) {
        val o = JSONObject()
            .put("matchesPlayed", stats.matchesPlayed)
            .put("matchesWon", stats.matchesWon)
            .put("roundsWon", stats.roundsWon)
            .put("highestScore", stats.highestScore)
            .put("totalPointsScored", stats.totalPointsScored)
        prefs.edit().putString(KEY_STATS, o.toString()).apply()
    }

    fun resetAllStats(): LifetimeStats {
        val fresh = LifetimeStats()
        saveStats(fresh)
        saveMatch(null)
        return fresh
    }

    fun saveMatch(match: MatchState?) {
        if (match == null || match.phase == GamePhase.GAME_OVER) {
            prefs.edit().remove(KEY_MATCH).apply()
            return
        }
        prefs.edit().putString(KEY_MATCH, encodeMatch(match).toString()).apply()
    }

    fun loadSavedMatch(): MatchState? {
        val raw = prefs.getString(KEY_MATCH, null) ?: return null
        return runCatching { decodeMatch(JSONObject(raw)) }.getOrNull()
    }

    private fun encodeTile(t: DominoTile): JSONObject =
        JSONObject().put("id", t.id).put("a", t.a).put("b", t.b)

    private fun decodeTile(o: JSONObject): DominoTile =
        DominoTile(o.getString("id"), o.getInt("a"), o.getInt("b"))

    private fun encodePlaced(p: PlacedTile): JSONObject =
        JSONObject()
            .put("tile", encodeTile(p.tile))
            .put("target", p.target.name)
            .put("innerPip", p.innerPip)
            .put("outerPip", p.outerPip)
            .put("isDouble", p.isDouble)
            .put("isSpinner", p.isSpinner)
            .put("playedBy", p.playedBy)
            .put("sequence", p.sequence)

    private fun decodePlaced(o: JSONObject): PlacedTile =
        PlacedTile(
            tile = decodeTile(o.getJSONObject("tile")),
            target = PlayTarget.valueOf(o.getString("target")),
            innerPip = o.getInt("innerPip"),
            outerPip = o.getInt("outerPip"),
            isDouble = o.getBoolean("isDouble"),
            isSpinner = o.getBoolean("isSpinner"),
            playedBy = o.getInt("playedBy"),
            sequence = o.getInt("sequence")
        )

    private fun encodeMatch(m: MatchState): JSONObject {
        val playersArr = JSONArray().apply {
            m.players.forEach { p ->
                put(
                    JSONObject()
                        .put("id", p.id)
                        .put("name", p.name)
                        .put("isAI", p.isAI)
                        .put("avatarIndex", p.avatarIndex)
                        .put("score", p.score)
                        .put("roundsWon", p.roundsWon)
                        .put("hand", JSONArray().apply { p.hand.forEach { put(encodeTile(it)) } })
                )
            }
        }
        val boneyardArr = JSONArray().apply { m.boneyard.forEach { put(encodeTile(it)) } }
        val boardObj = JSONObject()
            .put("root", m.board.root?.let(::encodePlaced) ?: JSONObject.NULL)
            .put("left", JSONArray().apply { m.board.left.forEach { put(encodePlaced(it)) } })
            .put("right", JSONArray().apply { m.board.right.forEach { put(encodePlaced(it)) } })
            .put("up", JSONArray().apply { m.board.up.forEach { put(encodePlaced(it)) } })
            .put("down", JSONArray().apply { m.board.down.forEach { put(encodePlaced(it)) } })
            .put("spinnerTileId", m.board.spinnerTileId ?: JSONObject.NULL)
            .put("spinnerLocation", m.board.spinnerLocation?.name ?: JSONObject.NULL)

        val summaryObj = m.roundSummary?.let { s ->
            JSONObject()
                .put("roundNumber", s.roundNumber)
                .put("winnerId", s.winnerId ?: JSONObject.NULL)
                .put("winnerName", s.winnerName)
                .put("winType", s.winType.id)
                .put("pointsAwarded", s.pointsAwarded)
                .put("matchWinnerId", s.matchWinnerId ?: JSONObject.NULL)
                .put("breakdowns", JSONArray().apply {
                    s.playerBreakdowns.forEach { b ->
                        put(
                            JSONObject()
                                .put("playerId", b.playerId)
                                .put("name", b.name)
                                .put("remainingPips", b.remainingPips)
                                .put("totalScore", b.totalScore)
                                .put("remainingTiles", JSONArray().apply {
                                    b.remainingTiles.forEach { put(encodeTile(it)) }
                                })
                        )
                    }
                })
        }

        return JSONObject()
            .put("phase", m.phase.name)
            .put("mode", m.mode.id)
            .put("playerCount", m.playerCount)
            .put("passAndPlay", m.passAndPlay)
            .put("targetScore", m.targetScore)
            .put("difficulty", m.difficulty.id)
            .put("roundNumber", m.roundNumber)
            .put("players", playersArr)
            .put("currentPlayerIndex", m.currentPlayerIndex)
            .put("boneyard", boneyardArr)
            .put("board", boardObj)
            .put("consecutivePasses", m.consecutivePasses)
            .put("moveSequence", m.moveSequence)
            .put("lastPlayedTileId", m.lastPlayedTileId ?: JSONObject.NULL)
            .put("matchWinnerId", m.matchWinnerId ?: JSONObject.NULL)
            .put("actionBanner", m.actionBanner ?: JSONObject.NULL)
            .put("roundSummary", summaryObj ?: JSONObject.NULL)
    }

    private fun decodeMatch(o: JSONObject): MatchState {
        val playersArr = o.getJSONArray("players")
        val players = (0 until playersArr.length()).map { i ->
            val p = playersArr.getJSONObject(i)
            val handArr = p.getJSONArray("hand")
            val hand = (0 until handArr.length()).map { j -> decodeTile(handArr.getJSONObject(j)) }
            Player(
                id = p.getInt("id"),
                name = p.getString("name"),
                isAI = p.getBoolean("isAI"),
                avatarIndex = p.getInt("avatarIndex"),
                hand = hand,
                score = p.getInt("score"),
                roundsWon = p.getInt("roundsWon")
            )
        }

        val boneyardArr = o.getJSONArray("boneyard")
        val boneyard = (0 until boneyardArr.length()).map { decodeTile(boneyardArr.getJSONObject(it)) }

        val b = o.getJSONObject("board")
        fun decodePlacedList(key: String): List<PlacedTile> {
            val arr = b.getJSONArray(key)
            return (0 until arr.length()).map { decodePlaced(arr.getJSONObject(it)) }
        }
        val board = BoardBranches(
            root = if (b.isNull("root")) null else decodePlaced(b.getJSONObject("root")),
            left = decodePlacedList("left"),
            right = decodePlacedList("right"),
            up = decodePlacedList("up"),
            down = decodePlacedList("down"),
            spinnerTileId = if (b.isNull("spinnerTileId")) null else b.getString("spinnerTileId"),
            spinnerLocation = if (b.isNull("spinnerLocation")) null else PlayTarget.valueOf(b.getString("spinnerLocation"))
        )

        val summary = if (o.isNull("roundSummary")) null else {
            val s = o.getJSONObject("roundSummary")
            val brArr = s.getJSONArray("breakdowns")
            val breakdowns = (0 until brArr.length()).map { i ->
                val br = brArr.getJSONObject(i)
                val tArr = br.getJSONArray("remainingTiles")
                PlayerRoundBreakdown(
                    playerId = br.getInt("playerId"),
                    name = br.getString("name"),
                    remainingPips = br.getInt("remainingPips"),
                    remainingTiles = (0 until tArr.length()).map { decodeTile(tArr.getJSONObject(it)) },
                    totalScore = br.getInt("totalScore")
                )
            }
            RoundSummary(
                roundNumber = s.getInt("roundNumber"),
                winnerId = if (s.isNull("winnerId")) null else s.getInt("winnerId"),
                winnerName = s.getString("winnerName"),
                winType = WinType.fromId(s.getString("winType")),
                pointsAwarded = s.getInt("pointsAwarded"),
                playerBreakdowns = breakdowns,
                matchWinnerId = if (s.isNull("matchWinnerId")) null else s.getInt("matchWinnerId")
            )
        }

        return MatchState(
            phase = GamePhase.valueOf(o.getString("phase")),
            mode = GameMode.fromId(o.getString("mode")),
            playerCount = o.getInt("playerCount"),
            passAndPlay = o.getBoolean("passAndPlay"),
            targetScore = o.getInt("targetScore"),
            difficulty = Difficulty.fromId(o.getString("difficulty")),
            roundNumber = o.getInt("roundNumber"),
            players = players,
            currentPlayerIndex = o.getInt("currentPlayerIndex"),
            boneyard = boneyard,
            board = board,
            consecutivePasses = o.getInt("consecutivePasses"),
            moveSequence = o.getInt("moveSequence"),
            lastPlayedTileId = if (o.isNull("lastPlayedTileId")) null else o.getString("lastPlayedTileId"),
            matchWinnerId = if (o.isNull("matchWinnerId")) null else o.getInt("matchWinnerId"),
            actionBanner = if (o.isNull("actionBanner")) null else o.getString("actionBanner"),
            roundSummary = summary
        )
    }
}
