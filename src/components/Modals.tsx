import React from 'react';
import { motion } from 'motion/react';
import { Award, ArrowRight, Eye, RotateCcw, Trophy } from 'lucide-react';
import { isRoundBasedMode, MODE_META } from '../engine/dominoEngine';
import { MatchState } from '../types/domino';
import { DominoTileView } from './DominoTileView';

interface PassDeviceModalProps {
  playerName: string;
  onReady: () => void;
}

export const PassDeviceModal: React.FC<PassDeviceModalProps> = ({
  playerName,
  onReady,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-md p-4">
      <motion.div
        initial={{ scale: 0.92, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="w-full max-w-sm rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 text-center shadow-2xl border border-black/5 dark:border-white/10"
      >
        <div className="w-12 h-12 rounded-2xl bg-teal-700/10 dark:bg-teal-400/15 text-teal-700 dark:text-teal-300 flex items-center justify-center mx-auto mb-4">
          <Eye className="w-6 h-6" />
        </div>
        <h2 className="font-display text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
          Pass Device to {playerName}
        </h2>
        <p className="mt-2 text-sm text-[#5A606D] dark:text-[#9AA5B8]">
          Keep your tiles private. Tap below when {playerName} is ready to play.
        </p>
        <button
          type="button"
          onClick={onReady}
          className="mt-6 w-full min-h-[48px] rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 font-semibold text-sm transition-colors"
        >
          Reveal {playerName}’s Hand
        </button>
      </motion.div>
    </div>
  );
};

interface ModeTipModalProps {
  match: MatchState;
  onDismiss: () => void;
}

export const ModeTipModal: React.FC<ModeTipModalProps> = ({ match, onDismiss }) => {
  const meta = MODE_META[match.mode];
  const roundBased = isRoundBasedMode(match.mode);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-sm p-4">
      <motion.div
        initial={{ scale: 0.94, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="w-full max-w-md rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 shadow-2xl border border-black/5 dark:border-white/10"
      >
        <p className="text-xs font-medium text-teal-700 dark:text-teal-400">
          {meta.name} ·{' '}
          {roundBased
            ? `First to ${match.targetScore} ${match.targetScore === 1 ? 'round' : 'rounds'}`
            : `Target ${match.targetScore} pts`}
        </p>
        <h2 className="font-display text-2xl font-bold text-[#171A21] dark:text-[#F5F2EB] mt-1">
          Quick Mode Primer
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-[#5A606D] dark:text-[#9AA5B8]">
          {meta.shortRule}
        </p>

        {/* Animated Mini Example */}
        <div className="mt-4 rounded-2xl bg-[#ECE6DA] dark:bg-[#0E131D] p-4 flex flex-col items-center">
          <div className="flex items-center gap-1.5">
            <motion.div
              animate={{ x: [0, 2, 0] }}
              transition={{ repeat: Infinity, duration: 2.4 }}
            >
              <DominoTileView
                tile={{ id: 'ex-1', a: 4, b: 6 }}
                orientation="horizontal"
                size="xs"
              />
            </motion.div>
            <DominoTileView
              tile={{ id: 'ex-2', a: 6, b: 6 }}
              orientation="vertical"
              size="xs"
              isSpinner={meta.hasSpinner}
            />
            <motion.div
              animate={{ x: [0, -2, 0] }}
              transition={{ repeat: Infinity, duration: 2.4 }}
            >
              <DominoTileView
                tile={{
                  id: 'ex-3',
                  a: 6,
                  b: match.mode === 'all_threes' || match.mode === 'fives_and_threes' ? 5 : 1,
                }}
                orientation="horizontal"
                size="xs"
              />
            </motion.div>
          </div>
          <p className="mt-3 text-xs font-mono-num font-medium text-[#171A21] dark:text-[#F5F2EB]">
            {match.mode === 'free_game' && 'Draw when stuck · Empty hand or lowest pips wins'}
            {match.mode === 'block' && 'No drawing · Empty hand or lowest pips wins'}
            {match.mode === 'all_fives' && 'Open ends 4 + 1 = 5 -> +5 pts immediately'}
            {match.mode === 'all_threes' && 'Open ends 4 + 5 = 9 -> +9 pts immediately'}
            {match.mode === 'fives_and_threes' && 'Open ends 4 + 5 = 9 -> 3×3 = +3 pts'}
          </p>
        </div>

        <button
          type="button"
          onClick={onDismiss}
          className="mt-5 w-full min-h-[48px] rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 font-semibold text-sm flex items-center justify-center gap-2 transition-colors"
        >
          Start Playing
          <ArrowRight className="w-4 h-4" />
        </button>
      </motion.div>
    </div>
  );
};

interface RoundSummaryModalProps {
  match: MatchState;
  onNextRound: () => void;
}

export const RoundSummaryModal: React.FC<RoundSummaryModalProps> = ({
  match,
  onNextRound,
}) => {
  const summary = match.roundSummary;
  if (!summary) return null;
  const roundBased = isRoundBasedMode(match.mode);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <motion.div
        initial={{ scale: 0.92, opacity: 0, y: 12 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        className="w-full max-w-md rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 shadow-2xl border border-black/5 dark:border-white/10 max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-2xl bg-teal-700/10 dark:bg-teal-400/15 text-teal-700 dark:text-teal-300 flex items-center justify-center shrink-0">
            <Award className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8]">
              Round {summary.roundNumber} Complete ·{' '}
              {summary.winType === 'domino'
                ? 'Domino Out'
                : summary.winType === 'blocked'
                ? 'Blocked Board'
                : 'Tie'}
            </p>
            <h2 className="font-display text-xl font-bold text-[#171A21] dark:text-[#F5F2EB]">
              {summary.winnerId !== null
                ? roundBased
                  ? `${summary.winnerName} wins the round`
                  : `${summary.winnerName} wins +${summary.pointsAwarded} pts`
                : 'Round Tied'}
            </h2>
          </div>
        </div>

        <div className="mt-5 divide-y divide-black/5 dark:divide-white/5">
          {summary.playerBreakdowns.map((b) => {
            const playerObj = match.players.find((p) => p.id === b.playerId);
            return (
              <div key={b.playerId} className="py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-[#171A21] dark:text-[#F5F2EB]">
                    {b.name}
                  </p>
                  <p className="text-xs text-[#5A606D] dark:text-[#9AA5B8] font-mono-num">
                    {b.remainingTiles.length === 0
                      ? 'Empty hand (0 pips)'
                      : `${b.remainingTiles.length} tiles · ${b.remainingPips} pips left`}
                  </p>
                </div>
                <span className="font-mono-num text-sm font-bold text-[#171A21] dark:text-[#F5F2EB] shrink-0">
                  {roundBased
                    ? `${playerObj?.roundsWon ?? 0} / ${match.targetScore}`
                    : `${b.totalScore} / ${match.targetScore}`}
                </span>
              </div>
            );
          })}
        </div>

        <button
          type="button"
          onClick={onNextRound}
          className="mt-5 w-full min-h-[48px] rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 font-semibold text-sm flex items-center justify-center gap-2 transition-colors"
        >
          Continue to Round {summary.roundNumber + 1}
          <ArrowRight className="w-4 h-4" />
        </button>
      </motion.div>
    </div>
  );
};

interface GameOverModalProps {
  match: MatchState;
  onPlayAgain: () => void;
  onBackToMenu: () => void;
}

export const GameOverModal: React.FC<GameOverModalProps> = ({
  match,
  onPlayAgain,
  onBackToMenu,
}) => {
  const roundBased = isRoundBasedMode(match.mode);
  const sorted = [...match.players].sort((a, b) =>
    roundBased ? b.roundsWon - a.roundsWon : b.score - a.score
  );
  const winner =
    match.players.find((p) => p.id === match.matchWinnerId) ?? sorted[0];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 backdrop-blur-md p-4">
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 16 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        className="w-full max-w-md rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 shadow-2xl border border-black/5 dark:border-white/10"
      >
        <div className="flex flex-col items-center text-center">
          <div className="w-14 h-14 rounded-3xl bg-amber-500/15 text-amber-600 dark:text-amber-400 flex items-center justify-center mb-3">
            <Trophy className="w-7 h-7" />
          </div>
          <p className="text-xs font-medium text-[#5A606D] dark:text-[#9AA5B8]">
            Game Over · {MODE_META[match.mode].name} (
            {roundBased
              ? `${match.targetScore} ${match.targetScore === 1 ? 'round' : 'rounds'} target`
              : `${match.targetScore} pt target`}
            )
          </p>
          <h2 className="font-display text-2xl font-bold text-[#171A21] dark:text-[#F5F2EB] mt-1">
            {winner.name} Wins!
          </h2>
          <p className="text-sm text-teal-700 dark:text-teal-400 font-semibold mt-0.5 font-mono-num">
            {roundBased
              ? `Rounds Won: ${winner.roundsWon} / ${match.targetScore}`
              : `Final Score: ${winner.score} pts`}
          </p>
        </div>

        <div className="mt-5 rounded-2xl bg-[#F2EEE6] dark:bg-[#0E131D] p-3 divide-y divide-black/5 dark:divide-white/5">
          {sorted.map((p, index) => (
            <div
              key={p.id}
              className="py-2.5 px-2 flex items-center justify-between gap-2"
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="font-mono-num text-xs font-bold w-5 text-[#5A606D] dark:text-[#9AA5B8]">
                  #{index + 1}
                </span>
                <span className="text-sm font-semibold text-[#171A21] dark:text-[#F5F2EB] truncate">
                  {p.name}
                </span>
              </div>
              <span className="font-mono-num text-sm font-bold text-[#171A21] dark:text-[#F5F2EB]">
                {roundBased
                  ? `${p.roundsWon} ${p.roundsWon === 1 ? 'round' : 'rounds'}`
                  : `${p.score} pts`}
              </span>
            </div>
          ))}
        </div>

        <div className="mt-5 grid grid-cols-2 gap-2.5">
          <button
            type="button"
            onClick={onBackToMenu}
            className="min-h-[48px] rounded-2xl bg-[#F2EEE6] dark:bg-[#1E283C] hover:bg-[#EAE4D8] dark:hover:bg-[#26324B] text-[#171A21] dark:text-[#F5F2EB] font-semibold text-sm transition-colors"
          >
            Mode Menu
          </button>
          <button
            type="button"
            onClick={onPlayAgain}
            className="min-h-[48px] rounded-2xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 font-semibold text-sm flex items-center justify-center gap-1.5 transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            Play Again
          </button>
        </div>
      </motion.div>
    </div>
  );
};

interface QuitGameModalProps {
  onCancel: () => void;
  onConfirmQuit: () => void;
}

export const QuitGameModal: React.FC<QuitGameModalProps> = ({
  onCancel,
  onConfirmQuit,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <motion.div
        initial={{ scale: 0.92, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="w-full max-w-sm rounded-3xl bg-[#FAF8F4] dark:bg-[#161E2E] p-6 shadow-2xl border border-black/5 dark:border-white/10"
      >
        <h2 className="font-display text-lg font-bold text-[#171A21] dark:text-[#F5F2EB]">
          Quit this game?
        </h2>
        <p className="mt-1.5 text-sm text-[#5A606D] dark:text-[#9AA5B8]">
          All progress will be lost.
        </p>
        <div className="mt-5 grid grid-cols-2 gap-2.5">
          <button
            type="button"
            onClick={onCancel}
            className="min-h-[46px] rounded-2xl bg-[#F2EEE6] dark:bg-[#1E283C] hover:bg-[#EAE4D8] dark:hover:bg-[#26324B] text-[#171A21] dark:text-[#F5F2EB] font-semibold text-sm transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirmQuit}
            className="min-h-[46px] rounded-2xl bg-rose-600 hover:bg-rose-700 text-white font-semibold text-sm transition-colors"
          >
            Quit
          </button>
        </div>
      </motion.div>
    </div>
  );
};
