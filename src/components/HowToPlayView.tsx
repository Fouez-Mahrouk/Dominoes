import React, { useState } from 'react';
import { motion } from 'motion/react';
import { GameMode } from '../types/domino';
import { DominoTileView } from './DominoTileView';

const MODE_GUIDES: {
  id: GameMode;
  title: string;
  tagline: string;
  rules: string[];
  exampleLabel: string;
}[] = [
  {
    id: 'free_game',
    title: 'Free Game',
    tagline: 'Open dominoes · Draw when stuck · Win by rounds',
    rules: [
      'Each player is dealt 7 tiles (remaining tiles stay in the boneyard for 2- and 3-player games). The highest double opens the board.',
      'Take turns matching either open end of the chain. There is no scoring on multiples — the goal is to empty your hand.',
      'If you cannot play, draw from the boneyard until you can play or it runs empty. If empty and still stuck, pass your turn.',
      'When the boneyard is empty and everyone passes, the player with the lowest pip total wins the round. First to reach 1, 3, 5, or 7 round wins takes the match.',
    ],
    exampleLabel: 'Empty your hand first or hold lowest pips to win the round',
  },
  {
    id: 'block',
    title: 'Block Dominoes',
    tagline: 'Pure strategy · No drawing from boneyard',
    rules: [
      'Each player starts with 7 tiles. Match the pip count on either open end of the chain.',
      'If you cannot play, you must Pass your turn.',
      'First player to empty their hand — or lowest pip total when blocked — wins 1 round.',
      'First player to reach the selected Rounds to Win target (1, 3, 5, or 7) wins the match.',
    ],
    exampleLabel: '6|4 connects to 4|1 -> Open ends are 6 and 1',
  },
  {
    id: 'all_fives',
    title: 'All Fives (Muggins)',
    tagline: 'Draw when stuck · Score multiples of 5',
    rules: [
      'Whenever the exposed open ends add up to a multiple of 5 (5, 10, 15, 20…), you immediately score that exact total.',
      'The first double played becomes a 4-way Spinner. Once both inline sides are covered, its top and bottom also open.',
      'If an exposed end tile is a double, both halves count toward the open total (e.g., 5|5 end counts as 10).',
      'If you have no match, draw from the boneyard until you find one or it runs empty.',
    ],
    exampleLabel: 'Open ends 4 + 6 = 10 -> +10 pts immediately',
  },
  {
    id: 'all_threes',
    title: 'All Threes',
    tagline: 'Draw when stuck · Score multiples of 3',
    rules: [
      'Plays just like All Fives with boneyard drawing and a 4-way Spinner.',
      'Score immediately whenever open ends sum to a multiple of 3 (3, 6, 9, 12, 15…).',
      'Round-end remaining pip difference is rounded to the nearest 3.',
    ],
    exampleLabel: 'Open ends 6 + 3 = 9 -> +9 pts immediately',
  },
  {
    id: 'fives_and_threes',
    title: 'Fives & Threes',
    tagline: 'Draw when stuck · 1 pt per 5 and 1 pt per 3',
    rules: [
      'Score 1 point for every 5 when open ends divide by 5, and 1 point for every 3 when open ends divide by 3.',
      'Hitting 15 scores both! (15 ÷ 5 = 3 pts plus 15 ÷ 3 = 5 pts -> 8 pts total).',
      'First player to reach the selected match target (50, 100, 150, or 200 points) wins immediately.',
    ],
    exampleLabel: 'Open ends total 15 -> 3 (fives) + 5 (threes) = +8 pts',
  },
];

export const HowToPlayView: React.FC = () => {
  const [activeMode, setActiveMode] = useState<GameMode>('free_game');
  const guide = MODE_GUIDES.find((g) => g.id === activeMode) ?? MODE_GUIDES[0];

  return (
    <div className="w-full max-w-3xl mx-auto px-4 sm:px-6 py-5 pb-24 space-y-6">
      <div>
        <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-[#171A21] dark:text-[#F5F2EB]">
          How to Play
        </h1>
        <p className="text-sm text-[#5A606D] dark:text-[#9AA5B8] mt-0.5">
          Tap a mode below to inspect scoring rules and animated tile examples
        </p>
      </div>

      {/* Mode Selector Tabs */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
        {MODE_GUIDES.map((m) => {
          const active = m.id === activeMode;
          return (
            <button
              key={m.id}
              type="button"
              onClick={() => setActiveMode(m.id)}
              className={[
                'min-h-[44px] px-3 rounded-2xl text-xs sm:text-sm font-semibold transition-colors whitespace-nowrap',
                active
                  ? 'bg-teal-700 text-white dark:bg-teal-500 dark:text-slate-950 shadow-sm'
                  : 'bg-[#F2EEE6] dark:bg-[#161E2E] text-[#5A606D] dark:text-[#9AA5B8] hover:text-[#171A21] dark:hover:text-[#F5F2EB]',
              ].join(' ')}
            >
              {m.title.replace(' Dominoes', '')}
            </button>
          );
        })}
      </div>

      {/* Interactive Animated Demonstration Card */}
      <div className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-5 space-y-5">
        <div>
          <h2 className="font-display text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
            {guide.title}
          </h2>
          <p className="text-xs font-medium text-teal-700 dark:text-teal-400 mt-0.5">
            {guide.tagline}
          </p>
        </div>

        {/* Animated Mini-Board */}
        <div className="rounded-2xl bg-[#ECE6DA] dark:bg-[#0E131D] p-5 flex flex-col items-center justify-center overflow-hidden">
          {(activeMode === 'free_game' || activeMode === 'block') && (
            <div className="flex items-center gap-1.5">
              <DominoTileView
                tile={{ id: 'demo-b1', a: 6, b: 4 }}
                orientation="horizontal"
                size="sm"
              />
              <motion.div
                animate={{ x: [22, 0, 0], opacity: [0.3, 1, 1] }}
                transition={{ repeat: Infinity, duration: 2.8, times: [0, 0.35, 1] }}
              >
                <DominoTileView
                  tile={{ id: 'demo-b2', a: 4, b: 1 }}
                  orientation="horizontal"
                  size="sm"
                  isNew
                />
              </motion.div>
            </div>
          )}

          {activeMode === 'all_fives' && (
            <div className="flex flex-col items-center gap-1.5">
              <motion.div
                animate={{ y: [-14, 0, 0], opacity: [0.3, 1, 1] }}
                transition={{ repeat: Infinity, duration: 3, times: [0, 0.35, 1] }}
              >
                <DominoTileView
                  tile={{ id: 'demo-f-top', a: 4, b: 5 }}
                  orientation="vertical"
                  size="xs"
                />
              </motion.div>
              <div className="flex items-center gap-1.5">
                <DominoTileView
                  tile={{ id: 'demo-f-left', a: 2, b: 5 }}
                  orientation="horizontal"
                  size="xs"
                />
                <DominoTileView
                  tile={{ id: 'demo-f-spin', a: 5, b: 5 }}
                  orientation="vertical"
                  size="xs"
                  isSpinner
                />
                <DominoTileView
                  tile={{ id: 'demo-f-right', a: 5, b: 4 }}
                  orientation="horizontal"
                  size="xs"
                />
              </div>
            </div>
          )}

          {activeMode === 'all_threes' && (
            <div className="flex items-center gap-1.5">
              <DominoTileView
                tile={{ id: 'demo-t1', a: 6, b: 2 }}
                orientation="horizontal"
                size="sm"
              />
              <DominoTileView
                tile={{ id: 'demo-t2', a: 2, b: 2 }}
                orientation="vertical"
                size="sm"
                isSpinner
              />
              <motion.div
                animate={{ x: [22, 0, 0], opacity: [0.3, 1, 1] }}
                transition={{ repeat: Infinity, duration: 2.8, times: [0, 0.35, 1] }}
              >
                <DominoTileView
                  tile={{ id: 'demo-t3', a: 2, b: 3 }}
                  orientation="horizontal"
                  size="sm"
                  isNew
                />
              </motion.div>
            </div>
          )}

          {activeMode === 'fives_and_threes' && (
            <div className="flex items-center gap-1.5">
              <DominoTileView
                tile={{ id: 'demo-ft1', a: 6, b: 6 }}
                orientation="vertical"
                size="sm"
                isSpinner
              />
              <motion.div
                animate={{ x: [22, 0, 0], opacity: [0.3, 1, 1] }}
                transition={{ repeat: Infinity, duration: 2.8, times: [0, 0.35, 1] }}
              >
                <DominoTileView
                  tile={{ id: 'demo-ft2', a: 6, b: 3 }}
                  orientation="horizontal"
                  size="sm"
                  isNew
                />
              </motion.div>
            </div>
          )}

          <p className="mt-4 text-xs font-mono-num font-semibold text-center text-[#171A21] dark:text-[#F5F2EB]">
            {guide.exampleLabel}
          </p>
        </div>

        {/* Rule List */}
        <ul className="space-y-2.5">
          {guide.rules.map((rule, idx) => (
            <li
              key={idx}
              className="flex items-start gap-3 text-sm leading-relaxed text-[#171A21] dark:text-[#F5F2EB]"
            >
              <span className="font-mono-num text-xs font-bold text-teal-700 dark:text-teal-400 mt-1 shrink-0">
                0{idx + 1}.
              </span>
              <span>{rule}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Controls Quick Reference */}
      <div className="rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 p-5 space-y-2">
        <h3 className="font-display text-base font-bold text-[#171A21] dark:text-[#F5F2EB]">
          Touch & Pointer Controls
        </h3>
        <p className="text-sm text-[#5A606D] dark:text-[#9AA5B8] leading-relaxed">
          Drag any highlighted tile directly onto a matching open board end, or tap a tile once to illuminate valid endpoints and tap the target end to place. When only one valid endpoint exists, tapping a selected tile again places it automatically.
        </p>
      </div>
    </div>
  );
};
