package com.dominoes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dominoes.app.model.GamePhase
import com.dominoes.app.ui.components.GameBoardScreen
import com.dominoes.app.ui.components.GameOverDialog
import com.dominoes.app.ui.components.ModeTipDialog
import com.dominoes.app.ui.components.PassDeviceDialog
import com.dominoes.app.ui.components.QuitGameDialog
import com.dominoes.app.ui.components.RoundSummaryDialog
import com.dominoes.app.ui.screens.HowToPlayScreen
import com.dominoes.app.ui.screens.ModeStartScreen
import com.dominoes.app.ui.screens.SettingsScreen
import com.dominoes.app.ui.theme.DominoesTheme
import com.dominoes.app.ui.theme.LocalDominoPalette
import com.dominoes.app.viewmodel.DominoViewModel
import com.dominoes.app.viewmodel.NavTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: DominoViewModel = viewModel()
            val activeTab by vm.activeTab.collectAsState()
            val settings by vm.settings.collectAsState()
            val stats by vm.stats.collectAsState()
            val savedMatch by vm.savedMatch.collectAsState()
            val match by vm.match.collectAsState()
            val isAIThinking by vm.isAIThinking.collectAsState()
            val showModeTip by vm.showModeTip.collectAsState()
            val showQuitConfirm by vm.showQuitConfirm.collectAsState()
            val roundWinCelebration by vm.roundWinCelebration.collectAsState()
            val isCelebratingRound by vm.isCelebratingRound.collectAsState()

            // System Back button during an active game goes straight back to Start Screen without confirmation
            BackHandler(enabled = match != null && activeTab == NavTab.GAME) {
                vm.quitToMenuImmediate()
            }

            DominoesTheme(themeMode = settings.theme) {
                val palette = LocalDominoPalette.current

                Scaffold(
                    containerColor = palette.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = palette.surfaceCard,
                            contentColor = palette.textPrimary
                        ) {
                            NavTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                val icon = when (tab) {
                                    NavTab.GAME -> Icons.Filled.Casino
                                    NavTab.RULES -> Icons.AutoMirrored.Filled.MenuBook
                                    NavTab.SETTINGS -> Icons.Filled.Settings
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { vm.selectTab(tab) },
                                    icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                                    label = { Text(text = tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.accentTeal,
                                        selectedTextColor = palette.accentTeal,
                                        indicatorColor = palette.accentTeal.copy(alpha = 0.14f),
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            NavTab.GAME -> {
                                val currentMatch = match
                                if (currentMatch != null) {
                                    GameBoardScreen(
                                        match = currentMatch,
                                        isAIThinking = isAIThinking,
                                        roundWinCelebration = roundWinCelebration,
                                        onPlayTile = vm::playTile,
                                        onDrawTile = vm::drawTile,
                                        onPassTurn = vm::passTurn,
                                        onSelectTileHaptic = vm::onTileSelectedHaptic,
                                        onOpenRuleTip = vm::openModeTip,
                                        onRequestQuit = vm::requestQuitDialog
                                    )
                                } else {
                                    ModeStartScreen(
                                        savedMatch = savedMatch,
                                        defaultDifficulty = settings.difficulty,
                                        onResumeMatch = vm::resumeSavedMatch,
                                        onDiscardSavedMatch = vm::discardSavedMatch,
                                        onStartMatch = vm::startNewMatch
                                    )
                                }
                            }
                            NavTab.RULES -> HowToPlayScreen()
                            NavTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                stats = stats,
                                onSelectTheme = { mode -> vm.updateSettings { it.copy(theme = mode) } },
                                onToggleVibration = vm::toggleVibration,
                                onToggleSound = vm::toggleSound,
                                onToggleModeTip = {
                                    vm.updateSettings { it.copy(showModeTipOnStart = !it.showModeTipOnStart) }
                                },
                                onSelectDefaultDifficulty = { diff ->
                                    vm.updateSettings { it.copy(difficulty = diff) }
                                },
                                onResetScores = vm::resetAllScores
                            )
                        }

                        // Modals during active game
                        val activeMatch = match
                        if (activeMatch != null && activeTab == NavTab.GAME) {
                            if (showModeTip) {
                                ModeTipDialog(
                                    match = activeMatch,
                                    onDismiss = vm::dismissModeTip
                                )
                            }
                            if (activeMatch.phase == GamePhase.PASS_DEVICE) {
                                PassDeviceDialog(
                                    playerName = activeMatch.players[activeMatch.currentPlayerIndex].name,
                                    onReady = vm::readyPassDevice
                                )
                            }
                            if (activeMatch.phase == GamePhase.ROUND_SUMMARY && !isCelebratingRound) {
                                RoundSummaryDialog(
                                    match = activeMatch,
                                    onNextRound = vm::startNextRound
                                )
                            }
                            if (activeMatch.phase == GamePhase.GAME_OVER && !isCelebratingRound) {
                                GameOverDialog(
                                    match = activeMatch,
                                    onPlayAgain = vm::playAgain,
                                    onBackToMenu = vm::quitToMenuImmediate
                                )
                            }
                            if (showQuitConfirm) {
                                QuitGameDialog(
                                    onCancel = vm::cancelQuitDialog,
                                    onConfirmQuit = vm::quitToMenuImmediate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
