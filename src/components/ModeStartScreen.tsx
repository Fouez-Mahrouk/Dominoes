import React, { useState } from 'react';
import { Play, RotateCcw, Trash2 } from 'lucide-react';
import { isRoundBasedMode, MODE_META } from '../engine/dominoEngine';
import {
  Difficulty,
  GameMode,
  MatchState,
  PlayerCount,
  TargetScore,
} from '../types/domino';
import { DominoTileView } from './DominoTileView';

interface ModeStartScreenProps {
  savedMatch: MatchState | null;
  defaultDifficulty: Difficulty;
  onResumeMatch: () => void;
  onDiscardSavedMatch: () => void;
  onStartMatch: (params: {
    mode: GameMode;
    playerCount: PlayerCount;
    passAndPlay: boolean;
    targetScore: TargetScore;
    difficulty: Difficulty;
  }) => void;
}

const SECONDARY_MODES: GameMode[] = [
  'block',
  'all_fives',
  'all_threes',
  'fives_and_threes',
];
const ROUND_TARGET_OPTIONS: TargetScore[] = [1, 3, 5, 7];
const POINT_TARGET_OPTIONS: TargetScore[] = [50, 100, 150, 200];
const PLAYER_COUNTS: PlayerCount[] = [2, 3, 4];
const DIFFICULTIES: { id: Difficulty; label: string }[] = [
  { id: 'easy', label: 'Easy' },
  { id: 'medium', label: 'Medium' },
  { id: 'hard', label: 'Hard' },
];

export const ModeStartScreen: React.FC<ModeStartScreenProps> = ({
  savedMatch,
  defaultDifficulty,
  onResumeMatch,
  onDiscardSavedMatch,
  onStartMatch,
}) => {
  const [selectedMode, setSelectedMode] = useState<GameMode>('free_game');
  const [playerCount, setPlayerCount] = useState<PlayerCount>(2);
  const [passAndPlay, setPassAndPlay] = useState(false);
  const [targetScore, setTargetScore] = useState<TargetScore>(3);
  const [difficulty, setDifficulty] = useState<Difficulty>(defaultDifficulty);

  const roundBased = isRoundBasedMode(selectedMode);
  const targetOptions = roundBased ? ROUND_TARGET_OPTIONS : POINT_TARGET_OPTIONS;

  const handleSelectMode = (mode: GameMode) => {
    setSelectedMode(mode);
    const nextIsRound = isRoundBasedMode(mode);
    if (nextIsRound && !ROUND_TARGET_OPTIONS.includes(targetScore)) {
      setTargetScore(3);
    } else if (!nextIsRound && !POINT_TARGET_OPTIONS.includes(targetScore)) {
      setTargetScore(100);
    }
  };

  const handlePlayerCountChange = (count: PlayerCount) => {
    setPlayerCount(count);
    if (count !== 4) {
      setPassAndPlay(false);
    }
  };

  return (
    <div className="w-full max-w-2xl mx-auto px-4 sm:px-6 py-6 pb-24 space-y-6">
      {/* Centered Brand Header & Signature */}
      <div className="flex flex-col items-center justify-center text-center pt-1">
        <div className="flex items-center justify-center gap-3">
          <div className="flex items-center" aria-hidden="true">
            <DominoTileView tile={{ id: 'logo-1', a: 5, b: 3 }} width={24} height={48} />
          </div>
          <h1 className="font-display text-3xl sm:text-4xl font-bold tracking-tight text-[#171A21] dark:text-[#F5F2EB]">
            Dominoes
          </h1>
        </div>
        <p className="mt-[5px] text-[12px] font-light tracking-[0.1em] text-[#171A21]/60 dark:text-[#F5F2EB]/60 leading-none">
          Fouez Mahrouk
        </p>
      </div>

      {/* Resume Unfinished Match */}
      {savedMatch && (
        <div className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-teal-700/25 dark:border-teal-400/25 p-3.5 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="font-display font-semibold text-sm text-[#171A21] dark:text-[#F5F2EB] truncate">
              {MODE_META[savedMatch.mode].name} · Round {savedMatch.roundNumber}
            </p>
            <p className="text-xs font-mono-num text-[#5A606D] dark:text-[#9AA5B8] truncate">
              {savedMatch.players
                .map((p) =>
                  isRoundBasedMode(savedMatch.mode)
                    ? `${p.name} ${p.roundsWon}`
                    : `${p.name} ${p.score}`
                )
                .join(' · ')}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={onDiscardSavedMatch}
              aria-label="Discard saved match"
              className="min-h-[44px] min-w-[44px] rounded-2xl bg-black/5 dark:bg-white/5 hover:bg-rose-500/15 text-[#5A606D] hover:text-rose-600 dark:text-[#9AA5B8] dark:hover:text-rose-400 transition-colors flex items-center justify-center"
            >
              <Trash2 className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={onResumeMatch}
              className="min-h-[44px] px-4 rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 text-sm font-semibold flex items-center gap-1.5 transition-colors whitespace-nowrap"
            >
              <RotateCcw className="w-4 h-4" />
              Resume
            </button>
          </div>
        </div>
      )}

      {/* Mode Picker — Free Game Full Width First, then 2x2 Grid */}
      <section aria-labelledby="mode-heading" className="space-y-2.5">
        <h2
          id="mode-heading"
          className="font-display text-sm font-semibold text-[#5A606D] dark:text-[#9AA5B8]"
        >
          Mode
        </h2>
        <div className="grid grid-cols-2 gap-2.5">
          {/* Primary Featured Mode: Free Game spanning the 2 columns beneath it */}
          <button
            type="button"
            onClick={() => handleSelectMode('free_game')}
            className={[
              'col-span-2 min-h-[62px] rounded-2xl px-6 py-3.5 text-center transition-all border font-display font-bold text-lg sm:text-xl tracking-tight whitespace-nowrap',
              selectedMode === 'free_game'
                ? 'bg-[#171A21] text-[#FAF8F4] border-[#171A21] dark:bg-[#F5F2EB] dark:text-[#0E131D] dark:border-[#F5F2EB] shadow-sm'
                : 'bg-[#F2EEE6] dark:bg-[#161E2E] text-[#171A21] dark:text-[#F5F2EB] border-black/5 dark:border-white/5 hover:bg-[#EAE4D8] dark:hover:bg-[#1B2436]',
            ].join(' ')}
          >
            {MODE_META.free_game.name}
          </button>

          {SECONDARY_MODES.map((modeId) => {
            const meta = MODE_META[modeId];
            const isSelected = selectedMode === modeId;
            return (
              <button
                key={modeId}
                type="button"
                onClick={() => handleSelectMode(modeId)}
                className={[
                  'min-h-[56px] rounded-2xl px-4 py-3 text-center transition-all border font-display font-bold text-base sm:text-lg whitespace-nowrap',
                  isSelected
                    ? 'bg-[#171A21] text-[#FAF8F4] border-[#171A21] dark:bg-[#F5F2EB] dark:text-[#0E131D] dark:border-[#F5F2EB] shadow-sm'
                    : 'bg-[#F2EEE6] dark:bg-[#161E2E] text-[#171A21] dark:text-[#F5F2EB] border-black/5 dark:border-white/5 hover:bg-[#EAE4D8] dark:hover:bg-[#1B2436]',
                ].join(' ')}
              >
                {meta.name}
              </button>
            );
          })}
        </div>
      </section>

      {/* Player Count, Target Picker & Difficulty */}
      <section className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-4 sm:p-5 space-y-4">
        {/* Players */}
        <div className="space-y-2">
          <span className="block font-display text-sm font-semibold text-[#5A606D] dark:text-[#9AA5B8]">
            Players
          </span>
          <div className="grid grid-cols-3 gap-2">
            {PLAYER_COUNTS.map((count) => {
              const active = playerCount === count;
              return (
                <button
                  key={count}
                  type="button"
                  onClick={() => handlePlayerCountChange(count)}
                  className={[
                    'min-h-[44px] rounded-2xl text-sm font-semibold transition-colors whitespace-nowrap',
                    active
                      ? 'bg-[#171A21] text-[#FAF8F4] dark:bg-[#F5F2EB] dark:text-[#0E131D] shadow-sm'
                      : 'bg-white/60 dark:bg-[#0E131D]/50 text-[#5A606D] dark:text-[#9AA5B8] hover:text-[#171A21] dark:hover:text-[#F5F2EB]',
                  ].join(' ')}
                >
                  {count} Players
                </button>
              );
            })}
          </div>

          {playerCount === 4 && (
            <div className="grid grid-cols-2 gap-2 pt-1">
              <button
                type="button"
                onClick={() => setPassAndPlay(false)}
                className={[
                  'min-h-[40px] rounded-xl text-xs font-semibold transition-colors whitespace-nowrap px-3',
                  !passAndPlay
                    ? 'bg-teal-700 text-white dark:bg-teal-500 dark:text-slate-950'
                    : 'bg-white/50 dark:bg-[#0E131D]/40 text-[#5A606D] dark:text-[#9AA5B8]',
                ].join(' ')}
              >
                1 Human + 3 AI
              </button>
              <button
                type="button"
                onClick={() => setPassAndPlay(true)}
                className={[
                  'min-h-[40px] rounded-xl text-xs font-semibold transition-colors whitespace-nowrap px-3',
                  passAndPlay
                    ? 'bg-teal-700 text-white dark:bg-teal-500 dark:text-slate-950'
                    : 'bg-white/50 dark:bg-[#0E131D]/40 text-[#5A606D] dark:text-[#9AA5B8]',
                ].join(' ')}
              >
                Pass & Play
              </button>
            </div>
          )}
        </div>

        {/* Dynamic Target Picker: "Rounds to Win" (1/3/5/7) vs "Target Score" (50/100/150/200) */}
        <div className="space-y-2">
          <span className="block font-display text-sm font-semibold text-[#5A606D] dark:text-[#9AA5B8]">
            {roundBased ? 'Rounds to Win' : 'Target Score'}
          </span>
          <div className="grid grid-cols-4 gap-2">
            {targetOptions.map((val) => {
              const active = targetScore === val;
              return (
                <button
                  key={val}
                  type="button"
                  onClick={() => setTargetScore(val)}
                  className={[
                    'min-h-[44px] rounded-2xl text-sm font-mono-num font-semibold transition-colors whitespace-nowrap',
                    active
                      ? 'bg-[#171A21] text-[#FAF8F4] dark:bg-[#F5F2EB] dark:text-[#0E131D] shadow-sm'
                      : 'bg-white/60 dark:bg-[#0E131D]/50 text-[#5A606D] dark:text-[#9AA5B8] hover:text-[#171A21] dark:hover:text-[#F5F2EB]',
                  ].join(' ')}
                >
                  {val}
                </button>
              );
            })}
          </div>
        </div>

        {/* AI Level */}
        <div className="space-y-2">
          <span className="block font-display text-sm font-semibold text-[#5A606D] dark:text-[#9AA5B8]">
            AI Level
          </span>
          <div className="grid grid-cols-3 gap-2">
            {DIFFICULTIES.map((d) => {
              const active = difficulty === d.id;
              return (
                <button
                  key={d.id}
                  type="button"
                  onClick={() => setDifficulty(d.id)}
                  className={[
                    'min-h-[44px] rounded-2xl text-sm font-semibold transition-colors whitespace-nowrap',
                    active
                      ? 'bg-[#171A21] text-[#FAF8F4] dark:bg-[#F5F2EB] dark:text-[#0E131D] shadow-sm'
                      : 'bg-white/60 dark:bg-[#0E131D]/50 text-[#5A606D] dark:text-[#9AA5B8] hover:text-[#171A21] dark:hover:text-[#F5F2EB]',
                  ].join(' ')}
                >
                  {d.label}
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {/* Start Match Button */}
      <button
        type="button"
        onClick={() =>
          onStartMatch({
            mode: selectedMode,
            playerCount,
            passAndPlay: playerCount === 4 && passAndPlay,
            targetScore,
            difficulty,
          })
        }
        className="w-full min-h-[54px] rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 font-display font-bold text-base flex items-center justify-center gap-2 shadow-lg shadow-teal-900/15 active:scale-[0.99] transition-all"
      >
        <Play className="w-5 h-5 fill-current" />
        Start Match
      </button>
    </div>
  );
};
