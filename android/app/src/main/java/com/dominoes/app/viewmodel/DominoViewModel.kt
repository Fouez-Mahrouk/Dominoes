package com.dominoes.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dominoes.app.data.LocalStorage
import com.dominoes.app.engine.AIDecision
import com.dominoes.app.engine.DominoEngine
import com.dominoes.app.feedback.HapticKind
import com.dominoes.app.feedback.HapticManager
import com.dominoes.app.feedback.SoundManager
import com.dominoes.app.model.Difficulty
import com.dominoes.app.model.GameMode
import com.dominoes.app.model.GamePhase
import com.dominoes.app.model.GameSettings
import com.dominoes.app.model.LifetimeStats
import com.dominoes.app.model.MatchState
import com.dominoes.app.model.PlayTarget
import com.dominoes.app.model.RoundWinCelebration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NavTab(val label: String) {
    GAME("Game"),
    RULES("How to Play"),
    SETTINGS("Settings")
}

class DominoViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = LocalStorage(application)
    private val haptics = HapticManager(application)
    private val sound = SoundManager()

    private val _activeTab = MutableStateFlow(NavTab.GAME)
    val activeTab: StateFlow<NavTab> = _activeTab.asStateFlow()

    private val _settings = MutableStateFlow(storage.loadSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _stats = MutableStateFlow(storage.loadStats())
    val stats: StateFlow<LifetimeStats> = _stats.asStateFlow()

    private val _savedMatch = MutableStateFlow(storage.loadSavedMatch())
    val savedMatch: StateFlow<MatchState?> = _savedMatch.asStateFlow()

    private val _match = MutableStateFlow<MatchState?>(null)
    val match: StateFlow<MatchState?> = _match.asStateFlow()

    private val _isAIThinking = MutableStateFlow(false)
    val isAIThinking: StateFlow<Boolean> = _isAIThinking.asStateFlow()

    private val _showModeTip = MutableStateFlow(false)
    val showModeTip: StateFlow<Boolean> = _showModeTip.asStateFlow()

    private val _showQuitConfirm = MutableStateFlow(false)
    val showQuitConfirm: StateFlow<Boolean> = _showQuitConfirm.asStateFlow()

    private val _roundWinCelebration = MutableStateFlow<RoundWinCelebration?>(null)
    val roundWinCelebration: StateFlow<RoundWinCelebration?> = _roundWinCelebration.asStateFlow()

    private val _isCelebratingRound = MutableStateFlow(false)
    val isCelebratingRound: StateFlow<Boolean> = _isCelebratingRound.asStateFlow()

    private var aiJob: Job? = null
    private var celebrationJob: Job? = null
    private var celebratedRoundKey: String? = null
    private var recordedMatchEnd = false

    fun selectTab(tab: NavTab) {
        _activeTab.value = tab
        evaluateSideEffects()
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        storage.saveSettings(next)
    }

    fun toggleVibration() {
        val nextEnabled = !_settings.value.vibrationEnabled
        updateSettings { it.copy(vibrationEnabled = nextEnabled) }
        if (nextEnabled) {
            haptics.trigger(HapticKind.SCORE, true)
        }
    }

    fun toggleSound() {
        val nextEnabled = !_settings.value.soundEnabled
        updateSettings { it.copy(soundEnabled = nextEnabled) }
        if (nextEnabled) {
            sound.playPlace(true)
        }
    }

    fun onTileSelectedHaptic() {
        val s = _settings.value
        haptics.trigger(HapticKind.PICKUP, s.vibrationEnabled)
        sound.playPickup(s.soundEnabled)
    }

    fun startNewMatch(
        mode: GameMode,
        playerCount: Int,
        passAndPlay: Boolean,
        targetScore: Int,
        difficulty: Difficulty
    ) {
        recordedMatchEnd = false
        celebratedRoundKey = null
        val fresh = DominoEngine.createNewMatch(
            mode = mode,
            playerCount = playerCount,
            passAndPlay = passAndPlay,
            targetScore = targetScore,
            difficulty = difficulty
        )
        setMatchState(fresh)
        _activeTab.value = NavTab.GAME
        _showModeTip.value = _settings.value.showModeTipOnStart
        onTileSelectedHaptic()
        evaluateSideEffects()
    }

    fun resumeSavedMatch() {
        val saved = _savedMatch.value ?: return
        recordedMatchEnd = false
        celebratedRoundKey = null
        setMatchState(saved)
        _activeTab.value = NavTab.GAME
        haptics.trigger(HapticKind.PICKUP, _settings.value.vibrationEnabled)
        evaluateSideEffects()
    }

    fun discardSavedMatch() {
        storage.saveMatch(null)
        _savedMatch.value = null
    }

    fun dismissModeTip() {
        _showModeTip.value = false
        evaluateSideEffects()
    }

    fun openModeTip() {
        _showModeTip.value = true
        evaluateSideEffects()
    }

    fun requestQuitDialog() {
        _showQuitConfirm.value = true
    }

    fun cancelQuitDialog() {
        _showQuitConfirm.value = false
    }

    fun quitToMenuImmediate() {
        aiJob?.cancel()
        celebrationJob?.cancel()
        _match.value = null
        storage.saveMatch(null)
        _savedMatch.value = null
        _showQuitConfirm.value = false
        _showModeTip.value = false
        _isAIThinking.value = false
        _roundWinCelebration.value = null
        _isCelebratingRound.value = false
        celebratedRoundKey = null
        recordedMatchEnd = false
    }

    fun readyPassDevice() {
        val cur = _match.value ?: return
        if (cur.phase == GamePhase.PASS_DEVICE) {
            setMatchState(cur.copy(phase = GamePhase.PLAYING))
            evaluateSideEffects()
        }
    }

    fun playTile(tileId: String, target: PlayTarget) {
        val cur = _match.value ?: return
        if (cur.phase != GamePhase.PLAYING) return
        val next = DominoEngine.applyMove(cur, tileId, target)
        playTransitionFeedback(cur, next)
        setMatchState(next)
        evaluateSideEffects()
    }

    fun drawTile() {
        val cur = _match.value ?: return
        if (cur.phase != GamePhase.PLAYING) return
        val s = _settings.value
        sound.playDraw(s.soundEnabled)
        haptics.trigger(HapticKind.PICKUP, s.vibrationEnabled)
        setMatchState(DominoEngine.drawTileFromBoneyard(cur))
        evaluateSideEffects()
    }

    fun passTurn() {
        val cur = _match.value ?: return
        if (cur.phase != GamePhase.PLAYING) return
        val s = _settings.value
        sound.playPass(s.soundEnabled)
        haptics.trigger(HapticKind.PICKUP, s.vibrationEnabled)
        setMatchState(DominoEngine.passCurrentTurn(cur))
        evaluateSideEffects()
    }

    fun startNextRound() {
        val cur = _match.value ?: return
        val starterId = cur.roundSummary?.winnerId ?: cur.players.first().id
        val nextRound = DominoEngine.dealRound(
            cur.copy(roundNumber = cur.roundNumber + 1),
            roundStarterId = starterId
        )
        sound.playDraw(_settings.value.soundEnabled)
        setMatchState(nextRound)
        evaluateSideEffects()
    }

    fun playAgain() {
        val cur = _match.value ?: return
        recordedMatchEnd = false
        celebratedRoundKey = null
        val fresh = DominoEngine.createNewMatch(
            mode = cur.mode,
            playerCount = cur.playerCount,
            passAndPlay = cur.passAndPlay,
            targetScore = cur.targetScore,
            difficulty = cur.difficulty
        )
        sound.playDraw(_settings.value.soundEnabled)
        setMatchState(fresh)
        evaluateSideEffects()
    }

    fun resetAllScores() {
        val freshStats = storage.resetAllStats()
        _stats.value = freshStats
        quitToMenuImmediate()
    }

    private fun setMatchState(next: MatchState) {
        _match.value = next
        storage.saveMatch(next)
        _savedMatch.value = if (next.phase != GamePhase.GAME_OVER) next else null

        if (next.phase == GamePhase.GAME_OVER && !recordedMatchEnd) {
            recordedMatchEnd = true
            val human = next.players.first()
            val didWin = next.matchWinnerId == human.id
            val updated = _stats.value.let { prev ->
                prev.copy(
                    matchesPlayed = prev.matchesPlayed + 1,
                    matchesWon = prev.matchesWon + if (didWin) 1 else 0,
                    roundsWon = prev.roundsWon + human.roundsWon,
                    highestScore = maxOf(prev.highestScore, human.score),
                    totalPointsScored = prev.totalPointsScored + human.score
                )
            }
            _stats.value = updated
            storage.saveStats(updated)
        } else if (next.phase != GamePhase.GAME_OVER) {
            recordedMatchEnd = false
        }
    }

    private fun playTransitionFeedback(prev: MatchState, next: MatchState) {
        val s = _settings.value
        if (next.phase == GamePhase.GAME_OVER) {
            sound.playWin(s.soundEnabled)
            haptics.trigger(HapticKind.WIN, s.vibrationEnabled)
            return
        }
        if (next.phase == GamePhase.ROUND_SUMMARY && next.roundSummary?.winnerId != null) {
            sound.playScore(s.soundEnabled)
            haptics.trigger(HapticKind.SCORE, s.vibrationEnabled)
            return
        }
        if (next.lastScoringEvent != null && next.lastScoringEvent.id != prev.lastScoringEvent?.id) {
            sound.playScore(s.soundEnabled)
            haptics.trigger(HapticKind.SCORE, s.vibrationEnabled)
            return
        }
        if (next.moveSequence > prev.moveSequence) {
            sound.playPlace(s.soundEnabled)
            haptics.trigger(HapticKind.PLACE, s.vibrationEnabled)
        }
    }

    private fun evaluateSideEffects() {
        val cur = _match.value
        if (cur == null) {
            aiJob?.cancel()
            celebrationJob?.cancel()
            _isAIThinking.value = false
            _roundWinCelebration.value = null
            _isCelebratingRound.value = false
            return
        }

        // Round-win celebration trigger across all 5 modes
        val isRoundEnd = cur.phase == GamePhase.ROUND_SUMMARY || cur.phase == GamePhase.GAME_OVER
        val summary = cur.roundSummary
        if (isRoundEnd && summary != null && summary.winnerId != null) {
            val key = "${cur.roundNumber}-${summary.winnerId}-${cur.phase.name}"
            if (celebratedRoundKey != key) {
                celebratedRoundKey = key
                celebrationJob?.cancel()
                _roundWinCelebration.value = RoundWinCelebration(
                    roundNumber = summary.roundNumber,
                    winnerId = summary.winnerId,
                    winnerName = summary.winnerName
                )
                _isCelebratingRound.value = true
                celebrationJob = viewModelScope.launch {
                    delay(1650L)
                    _isCelebratingRound.value = false
                    _roundWinCelebration.value = null
                }
            }
        } else if (cur.phase == GamePhase.PLAYING) {
            celebratedRoundKey = null
            celebrationJob?.cancel()
            _roundWinCelebration.value = null
            _isCelebratingRound.value = false
        }

        // AI turn execution
        aiJob?.cancel()
        val currentPlayer = cur.players.getOrNull(cur.currentPlayerIndex)
        if (cur.phase == GamePhase.PLAYING &&
            !_showModeTip.value &&
            _activeTab.value == NavTab.GAME &&
            currentPlayer != null &&
            currentPlayer.isAI
        ) {
            _isAIThinking.value = true
            val thinkDelay = if (cur.moveSequence == 0) 550L else 720L
            aiJob = viewModelScope.launch {
                delay(thinkDelay)
                val latest = _match.value ?: return@launch
                if (latest.phase != GamePhase.PLAYING) return@launch
                val active = latest.players.getOrNull(latest.currentPlayerIndex) ?: return@launch
                if (!active.isAI) return@launch

                when (val decision = DominoEngine.chooseAIMove(latest)) {
                    AIDecision.Draw -> {
                        sound.playDraw(_settings.value.soundEnabled)
                        setMatchState(DominoEngine.drawTileFromBoneyard(latest))
                    }
                    AIDecision.Pass -> {
                        sound.playPass(_settings.value.soundEnabled)
                        setMatchState(DominoEngine.passCurrentTurn(latest))
                    }
                    is AIDecision.Play -> {
                        val next = DominoEngine.applyMove(latest, decision.move.tile.id, decision.move.target)
                        playTransitionFeedback(latest, next)
                        setMatchState(next)
                    }
                }
                evaluateSideEffects()
            }
        } else {
            _isAIThinking.value = false
        }
    }
}
