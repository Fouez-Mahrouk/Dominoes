import React, { useState } from 'react';
import { Download, Monitor, Moon, Sun, Trash2, Vibrate, Volume2 } from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { Difficulty, GameSettings, LifetimeStats, ThemeMode } from '../types/domino';
import { triggerHaptic } from '../utils/haptics';
import { soundEngine } from '../utils/sound';

interface SettingsViewProps {
  settings: GameSettings;
  stats: LifetimeStats;
  onUpdateSettings: (patch: Partial<GameSettings>) => void;
  onResetScores: () => void;
}

const THEMES: { id: ThemeMode; label: string; icon: React.ReactNode }[] = [
  { id: 'system', label: 'System', icon: <Monitor className="w-4 h-4" /> },
  { id: 'light', label: 'Light', icon: <Sun className="w-4 h-4" /> },
  { id: 'dark', label: 'Dark', icon: <Moon className="w-4 h-4" /> },
];

const DIFFICULTIES: { id: Difficulty; label: string }[] = [
  { id: 'easy', label: 'Easy' },
  { id: 'medium', label: 'Medium' },
  { id: 'hard', label: 'Hard' },
];

export const SettingsView: React.FC<SettingsViewProps> = ({
  settings,
  stats,
  onUpdateSettings,
  onResetScores,
}) => {
  const [confirmReset, setConfirmReset] = useState(false);
  const [resetDone, setResetDone] = useState(false);
  const [showIOSGuide, setShowIOSGuide] = useState(false);
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();

  const handleToggleVibration = () => {
    const next = !settings.vibrationEnabled;
    onUpdateSettings({ vibrationEnabled: next });
    if (next) {
      triggerHaptic('score', true);
    }
  };

  const handleToggleSound = () => {
    const next = !settings.soundEnabled;
    onUpdateSettings({ soundEnabled: next });
    if (next) {
      soundEngine.playPlace(true);
    }
  };

  return (
    <div className="w-full max-w-2xl mx-auto px-4 sm:px-6 py-5 pb-24 space-y-6">
      <div>
        <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-[#171A21] dark:text-[#F5F2EB]">
          Settings
        </h1>
        <p className="text-sm text-[#5A606D] dark:text-[#9AA5B8] mt-0.5">
          Appearance, tactile feedback, AI behavior, and local match records
        </p>
      </div>

      {/* Theme Selector */}
      <section className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-5 space-y-3">
        <div>
          <h2 className="font-display text-base font-bold text-[#171A21] dark:text-[#F5F2EB]">
            Theme
          </h2>
          <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">
            Warm off-white with charcoal tiles in light mode · Deep navy with ivory tiles in dark mode
          </p>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {THEMES.map((t) => {
            const active = settings.theme === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => onUpdateSettings({ theme: t.id })}
                className={[
                  'min-h-[44px] rounded-2xl text-sm font-semibold flex items-center justify-center gap-2 transition-colors whitespace-nowrap',
                  active
                    ? 'bg-[#171A21] text-[#FAF8F4] dark:bg-[#F5F2EB] dark:text-[#0E131D] shadow-sm'
                    : 'bg-white/60 dark:bg-[#0E131D]/50 text-[#5A606D] dark:text-[#9AA5B8] hover:text-[#171A21] dark:hover:text-[#F5F2EB]',
                ].join(' ')}
              >
                {t.icon}
                {t.label}
              </button>
            );
          })}
        </div>
      </section>

      {/* Tactile & Audio */}
      <section className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 divide-y divide-black/5 dark:divide-white/5">
        <div className="p-5 flex items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <Vibrate className="w-5 h-5 text-teal-700 dark:text-teal-400 mt-0.5 shrink-0" />
            <div>
              <p className="text-sm font-semibold text-[#171A21] dark:text-[#F5F2EB]">
                Haptic Vibration
              </p>
              <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">
                Distinct pulses on tile pickup, placement, scoring, and victory
              </p>
            </div>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={settings.vibrationEnabled}
            onClick={handleToggleVibration}
            className={[
              'w-12 h-7 rounded-full p-1 transition-colors shrink-0',
              settings.vibrationEnabled
                ? 'bg-teal-700 dark:bg-teal-500'
                : 'bg-black/15 dark:bg-white/15',
            ].join(' ')}
          >
            <span
              className={[
                'block w-5 h-5 rounded-full bg-white shadow-sm transition-transform',
                settings.vibrationEnabled ? 'translate-x-5' : 'translate-x-0',
              ].join(' ')}
            />
          </button>
        </div>

        <div className="p-5 flex items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <Volume2 className="w-5 h-5 text-teal-700 dark:text-teal-400 mt-0.5 shrink-0" />
            <div>
              <p className="text-sm font-semibold text-[#171A21] dark:text-[#F5F2EB]">
                Sound Effects
              </p>
              <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">
                Offline synthesized tile clacks, chimes, and win chords
              </p>
            </div>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={settings.soundEnabled}
            onClick={handleToggleSound}
            className={[
              'w-12 h-7 rounded-full p-1 transition-colors shrink-0',
              settings.soundEnabled
                ? 'bg-teal-700 dark:bg-teal-500'
                : 'bg-black/15 dark:bg-white/15',
            ].join(' ')}
          >
            <span
              className={[
                'block w-5 h-5 rounded-full bg-white shadow-sm transition-transform',
                settings.soundEnabled ? 'translate-x-5' : 'translate-x-0',
              ].join(' ')}
            />
          </button>
        </div>

        <div className="p-5 flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-[#171A21] dark:text-[#F5F2EB]">
              Mode Tip at Match Start
            </p>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">
              Show a brief animated rule reminder before Round 1
            </p>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={settings.showModeTipOnStart}
            onClick={() =>
              onUpdateSettings({ showModeTipOnStart: !settings.showModeTipOnStart })
            }
            className={[
              'w-12 h-7 rounded-full p-1 transition-colors shrink-0',
              settings.showModeTipOnStart
                ? 'bg-teal-700 dark:bg-teal-500'
                : 'bg-black/15 dark:bg-white/15',
            ].join(' ')}
          >
            <span
              className={[
                'block w-5 h-5 rounded-full bg-white shadow-sm transition-transform',
                settings.showModeTipOnStart ? 'translate-x-5' : 'translate-x-0',
              ].join(' ')}
            />
          </button>
        </div>
      </section>

      {/* Default AI Difficulty */}
      <section className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-5 space-y-3">
        <h2 className="font-display text-base font-bold text-[#171A21] dark:text-[#F5F2EB]">
          Default AI Difficulty
        </h2>
        <div className="grid grid-cols-3 gap-2">
          {DIFFICULTIES.map((d) => {
            const active = settings.difficulty === d.id;
            return (
              <button
                key={d.id}
                type="button"
                onClick={() => onUpdateSettings({ difficulty: d.id })}
                className={[
                  'min-h-[44px] rounded-2xl text-sm font-semibold transition-colors whitespace-nowrap',
                  active
                    ? 'bg-[#171A21] text-[#FAF8F4] dark:bg-[#F5F2EB] dark:text-[#0E131D] shadow-sm'
                    : 'bg-white/60 dark:bg-[#0E131D]/50 text-[#5A606D] dark:text-[#9AA5B8]',
                ].join(' ')}
              >
                {d.label}
              </button>
            );
          })}
        </div>
      </section>

      {/* Lifetime Stats & Reset */}
      <section className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-5 space-y-4">
        <h2 className="font-display text-base font-bold text-[#171A21] dark:text-[#F5F2EB]">
          Offline Career Records
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">Matches</p>
            <p className="font-mono-num text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
              {stats.matchesPlayed}
            </p>
          </div>
          <div>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">Wins</p>
            <p className="font-mono-num text-xl font-bold text-teal-700 dark:text-teal-400">
              {stats.matchesWon}
            </p>
          </div>
          <div>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">Rounds Won</p>
            <p className="font-mono-num text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
              {stats.roundsWon}
            </p>
          </div>
          <div>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">Best Score</p>
            <p className="font-mono-num text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
              {stats.highestScore}
            </p>
          </div>
        </div>

        <div className="pt-2 border-t border-black/5 dark:border-white/5 flex flex-wrap items-center justify-between gap-3">
          {confirmReset ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  onResetScores();
                  setConfirmReset(false);
                  setResetDone(true);
                  window.setTimeout(() => setResetDone(false), 2000);
                }}
                className="min-h-[42px] px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition-colors"
              >
                Confirm Reset
              </button>
              <button
                type="button"
                onClick={() => setConfirmReset(false)}
                className="min-h-[42px] px-3 rounded-xl bg-white/60 dark:bg-[#0E131D]/50 text-xs font-medium text-[#5A606D] dark:text-[#9AA5B8]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmReset(true)}
              className="min-h-[42px] px-4 rounded-xl bg-rose-600/10 hover:bg-rose-600/20 text-rose-700 dark:text-rose-400 text-xs font-semibold flex items-center gap-2 transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              {resetDone ? 'Scores Reset!' : 'Reset Scores & Saved Match'}
            </button>
          )}

          {!isInstalled && isInstallable && (
            <button
              type="button"
              onClick={install}
              className="min-h-[42px] px-4 rounded-xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 text-xs font-semibold flex items-center gap-2 transition-colors"
            >
              <Download className="w-4 h-4" />
              Install Offline App
            </button>
          )}

          {!isInstalled && !isInstallable && isIOS && (
            <button
              type="button"
              onClick={() => setShowIOSGuide(true)}
              className="min-h-[42px] px-4 rounded-xl bg-white/60 dark:bg-[#0E131D]/50 text-xs font-semibold text-[#171A21] dark:text-[#F5F2EB]"
            >
              Install on iOS
            </button>
          )}
        </div>
      </section>

      {showIOSGuide && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 shadow-xl">
            <h3 className="font-display text-lg font-bold text-[#171A21] dark:text-[#F5F2EB]">
              Install Dominoes
            </h3>
            <p className="mt-2 text-sm text-[#5A606D] dark:text-[#9AA5B8]">
              1. Tap the Share icon in your browser toolbar.
              <br />
              2. Choose Add to Home Screen for full offline play.
            </p>
            <button
              type="button"
              onClick={() => setShowIOSGuide(false)}
              className="mt-4 w-full min-h-[44px] rounded-2xl bg-teal-700 text-white font-semibold text-sm"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
