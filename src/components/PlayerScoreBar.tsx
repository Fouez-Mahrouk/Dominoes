import React, { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { Bot, User } from 'lucide-react';
import { isRoundBasedMode } from '../engine/dominoEngine';
import { GameMode, Player } from '../types/domino';
import { computeHandLayout } from '../utils/handLayout';
import { DominoTileBackView } from './DominoTileView';

interface PlayerScoreBarProps {
  mode: GameMode;
  players: Player[];
  currentPlayerIndex: number;
  targetScore: number;
  isAIThinking: boolean;
  lastDrawnTileId: string | null;
  lastDrawnPlayerId: number | null;
  roundWinnerId?: number | null;
}

const CELEBRATION_ANGLES = [0, 60, 120, 180, 240, 300];

const AVATAR_TONES = [
  'bg-teal-700 text-white dark:bg-teal-400 dark:text-slate-950',
  'bg-amber-600 text-white dark:bg-amber-400 dark:text-slate-950',
  'bg-indigo-600 text-white dark:bg-indigo-400 dark:text-slate-950',
  'bg-rose-600 text-white dark:bg-rose-400 dark:text-slate-950',
];

const OpponentMiniHand: React.FC<{
  player: Player;
  lastDrawnTileId: string | null;
  lastDrawnPlayerId: number | null;
}> = ({ player, lastDrawnTileId, lastDrawnPlayerId }) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [availWidth, setAvailWidth] = useState(120);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const update = () => {
      const w = el.getBoundingClientRect().width;
      if (w > 0) setAvailWidth(w);
    };
    update();
    const obs = new ResizeObserver(update);
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  const layout = computeHandLayout(player.hand.length, availWidth, 12, 20, 3);

  return (
    <div
      ref={containerRef}
      style={{ gap: `${layout.gap}px` }}
      className="mt-1.5 h-[22px] w-full flex items-center justify-start overflow-visible"
    >
      <AnimatePresence initial={false}>
        {player.hand.map((tile) => {
          const isNewDraw =
            player.id === lastDrawnPlayerId && tile.id === lastDrawnTileId;

          return (
            <motion.div
              key={tile.id}
              layout
              initial={
                isNewDraw
                  ? { y: -28, opacity: 0, scale: 0.6 }
                  : { opacity: 0, scale: 0.8 }
              }
              animate={{
                y: 0,
                opacity: 1,
                scale: 1,
                width: layout.tileWidth,
                height: layout.tileHeight,
              }}
              exit={{ opacity: 0, scale: 0.6, y: 8 }}
              transition={{ type: 'spring', stiffness: 360, damping: 26 }}
              className="shrink-0 flex items-center justify-center"
            >
              <DominoTileBackView
                width={layout.tileWidth}
                height={layout.tileHeight}
              />
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
};

export const PlayerScoreBar: React.FC<PlayerScoreBarProps> = ({
  mode,
  players,
  currentPlayerIndex,
  targetScore,
  isAIThinking,
  lastDrawnTileId,
  lastDrawnPlayerId,
  roundWinnerId = null,
}) => {
  const roundBased = isRoundBasedMode(mode);
  const gridCols =
    players.length === 2
      ? 'grid-cols-2'
      : players.length === 3
      ? 'grid-cols-3'
      : 'grid-cols-2 sm:grid-cols-4';

  return (
    <div className={`grid ${gridCols} gap-2 w-full`}>
      {players.map((player, idx) => {
        const isActive = idx === currentPlayerIndex;
        const isRoundWinner = roundWinnerId !== null && player.id === roundWinnerId;
        const currentValue = roundBased ? player.roundsWon : player.score;
        const pct = Math.min(100, Math.round((currentValue / targetScore) * 100));
        const tone = AVATAR_TONES[player.avatarIndex % AVATAR_TONES.length];

        return (
          <div
            key={player.id}
            className={[
              'relative rounded-2xl px-2.5 py-2 transition-all duration-200 border',
              isRoundWinner
                ? 'bg-amber-50/95 dark:bg-amber-950/35 border-amber-400 dark:border-amber-300 ring-2 ring-amber-400/50 dark:ring-amber-300/40 shadow-md z-20'
                : isActive
                ? 'bg-white dark:bg-[#1E2738] border-teal-600/70 dark:border-teal-400/70 shadow-sm ring-1 ring-teal-600/25 dark:ring-teal-400/25'
                : 'bg-[#F2EEE6]/80 dark:bg-[#161E2E]/80 border-black/5 dark:border-white/5 opacity-85',
            ].join(' ')}
          >
            <div className="flex items-center gap-2">
              <div
                className={`relative w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${tone}`}
              >
                {player.isAI ? (
                  <Bot className="w-3.5 h-3.5" aria-hidden="true" />
                ) : (
                  <User className="w-3.5 h-3.5" aria-hidden="true" />
                )}
                {isActive && player.isAI && isAIThinking && !isRoundWinner && (
                  <span
                    aria-label="AI thinking"
                    className="absolute -inset-0.5 rounded-full border-2 border-teal-500 border-t-transparent animate-spin"
                  />
                )}

                {/* Celebratory "Round Won!" badge & particles over winner's avatar */}
                <AnimatePresence>
                  {isRoundWinner && (
                    <>
                      {CELEBRATION_ANGLES.map((deg) => (
                        <motion.span
                          key={deg}
                          initial={{ x: 0, y: 0, opacity: 1, scale: 0.9 }}
                          animate={{
                            x: Math.cos((deg * Math.PI) / 180) * 24,
                            y: Math.sin((deg * Math.PI) / 180) * 20,
                            opacity: 0,
                            scale: 0.3,
                          }}
                          transition={{ duration: 0.9, ease: 'easeOut' }}
                          className="pointer-events-none absolute w-1.5 h-1.5 rounded-full bg-amber-400 dark:bg-amber-300 z-30"
                        />
                      ))}
                      <motion.div
                        initial={{ opacity: 0, scale: 0.55, y: 6 }}
                        animate={{ opacity: 1, scale: 1, y: -14 }}
                        exit={{ opacity: 0, scale: 0.85, y: -20 }}
                        transition={{ type: 'spring', stiffness: 420, damping: 22 }}
                        className="pointer-events-none absolute left-1/2 -translate-x-1/2 top-0 z-30 px-2 py-0.5 rounded-full bg-amber-400 dark:bg-amber-300 text-slate-950 font-display font-bold text-[10px] leading-tight whitespace-nowrap shadow-md ring-2 ring-[#FAF8F4] dark:ring-[#161E2E]"
                      >
                        Round Won!
                      </motion.div>
                    </>
                  )}
                </AnimatePresence>
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1">
                  <span className="text-xs font-semibold truncate text-[#171A21] dark:text-[#F5F2EB]">
                    {player.name}
                  </span>
                  <span className="text-[11px] font-mono-num font-semibold text-[#171A21] dark:text-[#F5F2EB] shrink-0">
                    {currentValue}
                    <span className="text-[#5A606D] dark:text-[#9AA5B8] font-normal">
                      /{targetScore}
                    </span>
                  </span>
                </div>

                <div className="flex items-center justify-between gap-2 mt-0.5">
                  <span className="text-[10px] text-[#5A606D] dark:text-[#9AA5B8] truncate">
                    {isActive && player.isAI && isAIThinking
                      ? 'Thinking...'
                      : `${player.hand.length} ${player.hand.length === 1 ? 'tile' : 'tiles'}`}
                  </span>
                </div>
              </div>
            </div>

            {/* Auto-scaled miniature hand row for all opponent/AI players (and non-active humans) */}
            {(player.isAI || idx !== currentPlayerIndex) && (
              <OpponentMiniHand
                player={player}
                lastDrawnTileId={lastDrawnTileId}
                lastDrawnPlayerId={lastDrawnPlayerId}
              />
            )}

            {/* Target Progress Bar */}
            <div className="mt-1.5 h-1 w-full rounded-full bg-black/10 dark:bg-white/10 overflow-hidden">
              <div
                style={{ width: `${pct}%` }}
                className={[
                  'h-full rounded-full transition-all duration-300',
                  isActive
                    ? 'bg-teal-600 dark:bg-teal-400'
                    : 'bg-slate-400 dark:bg-slate-500',
                ].join(' ')}
              />
            </div>

            {isActive && (
              <div
                aria-hidden="true"
                className="absolute bottom-0 inset-x-3 h-[2.5px] rounded-t-full bg-teal-600 dark:bg-teal-400"
              />
            )}
          </div>
        );
      })}
    </div>
  );
};
