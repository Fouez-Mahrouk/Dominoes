import React, { useCallback, useEffect, useRef, useState } from 'react';
import { BookOpen, Gamepad2, Settings } from 'lucide-react';
import { GameBoard } from './components/GameBoard';
import { HowToPlayView } from './components/HowToPlayView';
import { ModeStartScreen } from './components/ModeStartScreen';
import {
  GameOverModal,
  ModeTipModal,
  PassDeviceModal,
  QuitGameModal,
  RoundSummaryModal,
} from './components/Modals';
import { SettingsView } from './components/SettingsView';
import {
  applyMove,
  chooseAIMove,
  createNewMatch,
  dealRound,
  drawTileFromBoneyard,
  passCurrentTurn,
} from './engine/dominoEngine';
import {
  BranchDir,
  Difficulty,
  GameMode,
  GameSettings,
  LifetimeStats,
  MatchState,
  PlayerCount,
  TargetScore,
} from './types/domino';
import { triggerHaptic } from './utils/haptics';
import { soundEngine } from './utils/sound';
import {
  loadSavedMatch,
  loadSettings,
  loadStats,
  resetAllStats,
  saveMatch,
  saveSettings,
  saveStats,
} from './utils/storage';

type NavTab = 'game' | 'rules' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('game');
  const [settings, setSettings] = useState<GameSettings>(() => loadSettings());
  const [stats, setStats] = useState<LifetimeStats>(() => loadStats());
  const [savedMatch, setSavedMatch] = useState<MatchState | null>(() => loadSavedMatch());
  const [match, setMatch] = useState<MatchState | null>(null);
  const [isAIThinking, setIsAIThinking] = useState(false);
  const [showModeTip, setShowModeTip] = useState(false);
  const [showQuitConfirm, setShowQuitConfirm] = useState(false);
  const [roundWinCelebration, setRoundWinCelebration] = useState<{
    roundNumber: number;
    winnerId: number;
    winnerName: string;
  } | null>(null);
  const [isCelebratingRound, setIsCelebratingRound] = useState(false);
  const recordedMatchEndRef = useRef<boolean>(false);
  const celebratedRoundKeyRef = useRef<string | null>(null);
  const matchRef = useRef<MatchState | null>(null);
  const pushedHistoryRef = useRef<boolean>(false);

  useEffect(() => {
    matchRef.current = match;
  }, [match]);

  // Sync theme (Light / Dark / System) to <html> class
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const applyTheme = () => {
      const isDark =
        settings.theme === 'dark' ||
        (settings.theme === 'system' && media.matches);
      document.documentElement.classList.toggle('dark', isDark);
    };
    applyTheme();
    media.addEventListener('change', applyTheme);
    return () => media.removeEventListener('change', applyTheme);
  }, [settings.theme]);

  // Persist settings
  const handleUpdateSettings = useCallback((patch: Partial<GameSettings>) => {
    setSettings((prev) => {
      const next = { ...prev, ...patch };
      saveSettings(next);
      return next;
    });
  }, []);

  // Persist active match for offline resume
  useEffect(() => {
    if (match) {
      saveMatch(match);
      if (match.phase !== 'GAME_OVER' && match.phase !== 'MENU') {
        setSavedMatch(match);
      } else if (match.phase === 'GAME_OVER') {
        setSavedMatch(null);
      }
    }
  }, [match]);

  // Record career stats once on Game Over
  useEffect(() => {
    if (!match || match.phase !== 'GAME_OVER') {
      recordedMatchEndRef.current = false;
      return;
    }
    if (recordedMatchEndRef.current) return;
    recordedMatchEndRef.current = true;

    const human = match.players[0];
    const didWin = match.matchWinnerId === human.id;
    setStats((prev) => {
      const next: LifetimeStats = {
        matchesPlayed: prev.matchesPlayed + 1,
        matchesWon: prev.matchesWon + (didWin ? 1 : 0),
        roundsWon: prev.roundsWon + human.roundsWon,
        highestScore: Math.max(prev.highestScore, human.score),
        totalPointsScored: prev.totalPointsScored + human.score,
      };
      saveStats(next);
      return next;
    });
  }, [match]);

  // Trigger brief celebratory overlay & "Round Won!" avatar badge on round wins across all modes
  useEffect(() => {
    if (!match) {
      celebratedRoundKeyRef.current = null;
      setRoundWinCelebration(null);
      setIsCelebratingRound(false);
      return;
    }

    const isRoundEndPhase =
      match.phase === 'ROUND_SUMMARY' || match.phase === 'GAME_OVER';
    const winnerId = match.roundSummary?.winnerId ?? null;

    if (isRoundEndPhase && winnerId !== null && match.roundSummary) {
      const key = `${match.roundNumber}-${winnerId}-${match.phase}`;
      if (celebratedRoundKeyRef.current === key) return;
      celebratedRoundKeyRef.current = key;

      setRoundWinCelebration({
        roundNumber: match.roundSummary.roundNumber,
        winnerId,
        winnerName: match.roundSummary.winnerName,
      });
      setIsCelebratingRound(true);

      const timer = window.setTimeout(() => {
        setIsCelebratingRound(false);
        setRoundWinCelebration(null);
      }, 1650);
      return () => window.clearTimeout(timer);
    }

    if (match.phase === 'PLAYING') {
      celebratedRoundKeyRef.current = null;
      setRoundWinCelebration(null);
      setIsCelebratingRound(false);
    }
  }, [match]);

  // Feedback helper on state transition
  const playTransitionFeedback = useCallback(
    (prev: MatchState, next: MatchState) => {
      if (next.phase === 'GAME_OVER') {
        soundEngine.playWin(settings.soundEnabled);
        triggerHaptic('win', settings.vibrationEnabled);
        return;
      }
      if (next.phase === 'ROUND_SUMMARY' && next.roundSummary?.winnerId !== null) {
        soundEngine.playScore(settings.soundEnabled);
        triggerHaptic('score', settings.vibrationEnabled);
        return;
      }
      if (
        next.lastScoringEvent &&
        next.lastScoringEvent.id !== prev.lastScoringEvent?.id
      ) {
        soundEngine.playScore(settings.soundEnabled);
        triggerHaptic('score', settings.vibrationEnabled);
        return;
      }
      if (next.moveSequence > prev.moveSequence) {
        soundEngine.playPlace(settings.soundEnabled);
        triggerHaptic('place', settings.vibrationEnabled);
      }
    },
    [settings.soundEnabled, settings.vibrationEnabled]
  );

  // AI turn automation with visible thinking spinner
  useEffect(() => {
    if (
      !match ||
      match.phase !== 'PLAYING' ||
      showModeTip ||
      activeTab !== 'game'
    ) {
      setIsAIThinking(false);
      return;
    }

    const current = match.players[match.currentPlayerIndex];
    if (!current || !current.isAI) {
      setIsAIThinking(false);
      return;
    }

    setIsAIThinking(true);
    const delay = match.moveSequence === 0 ? 550 : 720;

    const timer = window.setTimeout(() => {
      setMatch((prev) => {
        if (!prev || prev.phase !== 'PLAYING') return prev;
        const active = prev.players[prev.currentPlayerIndex];
        if (!active || !active.isAI) return prev;

        const decision = chooseAIMove(prev);
        if (decision === 'draw') {
          soundEngine.playDraw(settings.soundEnabled);
          return drawTileFromBoneyard(prev);
        }
        if (decision === 'pass') {
          soundEngine.playPass(settings.soundEnabled);
          return passCurrentTurn(prev);
        }

        const next = applyMove(prev, decision.tile.id, decision.branch);
        playTransitionFeedback(prev, next);
        return next;
      });
    }, delay);

    return () => window.clearTimeout(timer);
  }, [match, showModeTip, activeTab, settings.soundEnabled, playTransitionFeedback]);

  const discardAndExitToMenu = useCallback((popHistory: boolean) => {
    setMatch(null);
    saveMatch(null);
    setSavedMatch(null);
    setShowQuitConfirm(false);
    setShowModeTip(false);
    setIsAIThinking(false);
    setRoundWinCelebration(null);
    setIsCelebratingRound(false);
    celebratedRoundKeyRef.current = null;
    if (popHistory && pushedHistoryRef.current) {
      pushedHistoryRef.current = false;
      window.history.back();
    } else {
      pushedHistoryRef.current = false;
    }
  }, []);

  // System Back button during an active game returns straight to start screen without confirmation
  useEffect(() => {
    const onPopState = () => {
      if (matchRef.current) {
        discardAndExitToMenu(false);
      }
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, [discardAndExitToMenu]);

  const enterGameSession = (nextMatch: MatchState) => {
    setMatch(nextMatch);
    setActiveTab('game');
    if (!pushedHistoryRef.current) {
      window.history.pushState({ inGame: true }, '');
      pushedHistoryRef.current = true;
    }
  };

  const handleStartMatch = (params: {
    mode: GameMode;
    playerCount: PlayerCount;
    passAndPlay: boolean;
    targetScore: TargetScore;
    difficulty: Difficulty;
  }) => {
    const newMatch = createNewMatch(params);
    enterGameSession(newMatch);
    if (settings.showModeTipOnStart) {
      setShowModeTip(true);
    }
    triggerHaptic('pickup', settings.vibrationEnabled);
    soundEngine.playPickup(settings.soundEnabled);
  };

  const handleResumeMatch = () => {
    if (!savedMatch) return;
    enterGameSession(savedMatch);
    triggerHaptic('pickup', settings.vibrationEnabled);
  };

  const handleDiscardSavedMatch = () => {
    saveMatch(null);
    setSavedMatch(null);
  };

  const handlePlayTile = (tileId: string, branch: 'root' | BranchDir) => {
    setMatch((prev) => {
      if (!prev || prev.phase !== 'PLAYING') return prev;
      const next = applyMove(prev, tileId, branch);
      playTransitionFeedback(prev, next);
      return next;
    });
  };

  const handleDrawTile = () => {
    setMatch((prev) => {
      if (!prev || prev.phase !== 'PLAYING') return prev;
      soundEngine.playDraw(settings.soundEnabled);
      triggerHaptic('pickup', settings.vibrationEnabled);
      return drawTileFromBoneyard(prev);
    });
  };

  const handlePassTurn = () => {
    setMatch((prev) => {
      if (!prev || prev.phase !== 'PLAYING') return prev;
      soundEngine.playPass(settings.soundEnabled);
      triggerHaptic('pickup', settings.vibrationEnabled);
      return passCurrentTurn(prev);
    });
  };

  const handleNextRound = () => {
    setMatch((prev) => {
      if (!prev) return prev;
      const starterId = prev.roundSummary?.winnerId ?? prev.players[0].id;
      const nextRoundState = dealRound(
        { ...prev, roundNumber: prev.roundNumber + 1 },
        starterId
      );
      soundEngine.playDraw(settings.soundEnabled);
      return nextRoundState;
    });
  };

  const handlePlayAgain = () => {
    if (!match) return;
    const fresh = createNewMatch({
      mode: match.mode,
      playerCount: match.playerCount,
      passAndPlay: match.passAndPlay,
      targetScore: match.targetScore,
      difficulty: match.difficulty,
    });
    setMatch(fresh);
    soundEngine.playDraw(settings.soundEnabled);
  };

  const handleQuitToMenu = () => {
    discardAndExitToMenu(true);
  };

  const handleResetScores = () => {
    const freshStats = resetAllStats();
    setStats(freshStats);
    setSavedMatch(null);
    setMatch(null);
    setShowQuitConfirm(false);
    pushedHistoryRef.current = false;
  };

  const navItems: { id: NavTab; label: string; icon: React.ReactNode }[] = [
    { id: 'game', label: 'Game', icon: <Gamepad2 className="w-5 h-5" /> },
    { id: 'rules', label: 'How to Play', icon: <BookOpen className="w-5 h-5" /> },
    { id: 'settings', label: 'Settings', icon: <Settings className="w-5 h-5" /> },
  ];

  return (
    <div className="min-h-dvh h-dvh flex flex-col md:flex-row bg-[#FAF8F4] dark:bg-[#0E131D] text-[#171A21] dark:text-[#F5F2EB] overflow-hidden">
      {/* Desktop / Tablet Material 3 Navigation Rail */}
      <aside className="hidden md:flex flex-col items-center w-20 shrink-0 py-5 border-r border-black/5 dark:border-white/5 bg-[#F2EEE6]/70 dark:bg-[#161E2E]/70">
        <div className="w-10 h-10 rounded-2xl bg-teal-700 dark:bg-teal-500 text-white dark:text-slate-950 font-display font-bold text-lg flex items-center justify-center shadow-sm mb-8">
          D
        </div>
        <nav className="flex flex-col gap-3 w-full px-2">
          {navItems.map((item) => {
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={[
                  'flex flex-col items-center justify-center gap-1 py-2.5 px-1 rounded-2xl transition-colors min-h-[56px]',
                  active
                    ? 'bg-teal-700/15 text-teal-800 dark:bg-teal-400/20 dark:text-teal-300 font-semibold'
                    : 'text-[#5A606D] dark:text-[#9AA5B8] hover:bg-black/5 dark:hover:bg-white/5',
                ].join(' ')}
              >
                {item.icon}
                <span className="text-[10px] leading-tight text-center">
                  {item.label}
                </span>
              </button>
            );
          })}
        </nav>
      </aside>

      {/* Main Content Viewport */}
      <main className="flex-1 min-h-0 overflow-y-auto flex flex-col">
        {activeTab === 'game' &&
          (match ? (
            <GameBoard
              match={match}
              settings={settings}
              isAIThinking={isAIThinking}
              roundWinCelebration={roundWinCelebration}
              onPlayTile={handlePlayTile}
              onDrawTile={handleDrawTile}
              onPassTurn={handlePassTurn}
              onSelectTileHaptic={() => {
                triggerHaptic('pickup', settings.vibrationEnabled);
                soundEngine.playPickup(settings.soundEnabled);
              }}
              onOpenRuleTip={() => setShowModeTip(true)}
              onRequestQuit={() => setShowQuitConfirm(true)}
            />
          ) : (
            <ModeStartScreen
              savedMatch={savedMatch}
              defaultDifficulty={settings.difficulty}
              onResumeMatch={handleResumeMatch}
              onDiscardSavedMatch={handleDiscardSavedMatch}
              onStartMatch={handleStartMatch}
            />
          ))}

        {activeTab === 'rules' && <HowToPlayView />}

        {activeTab === 'settings' && (
          <SettingsView
            settings={settings}
            stats={stats}
            onUpdateSettings={handleUpdateSettings}
            onResetScores={handleResetScores}
          />
        )}
      </main>

      {/* Mobile Material 3 Bottom Navigation Bar (compact 60px height, < 10% viewport) */}
      <nav
        aria-label="Primary navigation"
        className="md:hidden shrink-0 h-[60px] border-t border-black/5 dark:border-white/5 bg-[#F2EEE6]/95 dark:bg-[#161E2E]/95 backdrop-blur-md grid grid-cols-3 items-center px-2"
      >
        {navItems.map((item) => {
          const active = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setActiveTab(item.id)}
              className="flex flex-col items-center justify-center min-h-[48px] gap-0.5 transition-colors"
            >
              <span
                className={[
                  'px-4 py-1 rounded-full transition-colors',
                  active
                    ? 'bg-teal-700/15 text-teal-800 dark:bg-teal-400/20 dark:text-teal-300'
                    : 'text-[#5A606D] dark:text-[#9AA5B8]',
                ].join(' ')}
              >
                {item.icon}
              </span>
              <span
                className={[
                  'text-[10px] font-semibold tracking-tight whitespace-nowrap',
                  active
                    ? 'text-teal-800 dark:text-teal-300'
                    : 'text-[#5A606D] dark:text-[#9AA5B8]',
                ].join(' ')}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </nav>

      {/* Modals */}
      {match && showModeTip && activeTab === 'game' && (
        <ModeTipModal match={match} onDismiss={() => setShowModeTip(false)} />
      )}

      {match && match.phase === 'PASS_DEVICE' && activeTab === 'game' && (
        <PassDeviceModal
          playerName={match.players[match.currentPlayerIndex].name}
          onReady={() =>
            setMatch((prev) =>
              prev ? { ...prev, phase: 'PLAYING' } : prev
            )
          }
        />
      )}

      {match && match.phase === 'ROUND_SUMMARY' && !isCelebratingRound && activeTab === 'game' && (
        <RoundSummaryModal match={match} onNextRound={handleNextRound} />
      )}

      {match && match.phase === 'GAME_OVER' && !isCelebratingRound && activeTab === 'game' && (
        <GameOverModal
          match={match}
          onPlayAgain={handlePlayAgain}
          onBackToMenu={handleQuitToMenu}
        />
      )}

      {match && showQuitConfirm && (
        <QuitGameModal
          onCancel={() => setShowQuitConfirm(false)}
          onConfirmQuit={() => discardAndExitToMenu(true)}
        />
      )}
    </div>
  );
}
