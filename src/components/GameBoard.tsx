import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { Layers, HelpCircle, SkipForward, X } from 'lucide-react';
import { computeBoardLayout } from '../engine/boardLayout';
import {
  getLegalMoves,
  getOpenEnds,
  getOpenEndsSum,
  isRoundBasedMode,
  MODE_META,
  tileCanMatchEnd,
} from '../engine/dominoEngine';
import { BranchDir, DominoTile, GameSettings, MatchState } from '../types/domino';
import { computeHandLayout } from '../utils/handLayout';
import { DominoTileView } from './DominoTileView';
import { PlayerScoreBar } from './PlayerScoreBar';

interface GameBoardProps {
  match: MatchState;
  settings: GameSettings;
  isAIThinking: boolean;
  roundWinCelebration?: {
    roundNumber: number;
    winnerId: number;
    winnerName: string;
  } | null;
  onPlayTile: (tileId: string, branch: 'root' | BranchDir) => void;
  onDrawTile: () => void;
  onPassTurn: () => void;
  onSelectTileHaptic: () => void;
  onOpenRuleTip: () => void;
  onRequestQuit: () => void;
}

interface DragState {
  tile: DominoTile;
  startX: number;
  startY: number;
  currentX: number;
  currentY: number;
  active: boolean;
}

const SPARKLE_ANGLES = [0, 45, 90, 135, 180, 225, 270, 315];

export const GameBoard: React.FC<GameBoardProps> = ({
  match,
  isAIThinking,
  roundWinCelebration = null,
  onPlayTile,
  onDrawTile,
  onPassTurn,
  onSelectTileHaptic,
  onOpenRuleTip,
  onRequestQuit,
}) => {
  const boardRef = useRef<HTMLDivElement | null>(null);
  const handRef = useRef<HTMLDivElement | null>(null);
  const dropRefs = useRef<Map<string, HTMLElement>>(new Map());

  const [viewport, setViewport] = useState({ width: 360, height: 320 });
  const [handWidth, setHandWidth] = useState(340);
  const [selectedTileId, setSelectedTileId] = useState<string | null>(null);
  const [drag, setDrag] = useState<DragState | null>(null);
  const [hoveredBranch, setHoveredBranch] = useState<'root' | BranchDir | null>(null);
  const [activeScoreBanner, setActiveScoreBanner] = useState<MatchState['lastScoringEvent']>(null);

  const currentPlayer = match.players[match.currentPlayerIndex];
  const isHumanTurn = !currentPlayer.isAI && match.phase === 'PLAYING';
  const modeMeta = MODE_META[match.mode];

  // Clear selection whenever turn changes
  useEffect(() => {
    setSelectedTileId(null);
    setDrag(null);
    setHoveredBranch(null);
  }, [match.currentPlayerIndex, match.moveSequence, match.roundNumber]);

  // Trigger brief floating score & sparkle burst when scoring occurs
  useEffect(() => {
    if (!match.lastScoringEvent) return;
    setActiveScoreBanner(match.lastScoringEvent);
    const timer = window.setTimeout(() => {
      setActiveScoreBanner(null);
    }, 1500);
    return () => window.clearTimeout(timer);
  }, [match.lastScoringEvent]);

  // Measure board and hand containers for responsive auto-fit
  useEffect(() => {
    const boardEl = boardRef.current;
    const handEl = handRef.current;
    const update = () => {
      if (boardEl) {
        const rect = boardEl.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          setViewport({ width: rect.width, height: rect.height });
        }
      }
      if (handEl) {
        const hRect = handEl.getBoundingClientRect();
        if (hRect.width > 0) {
          setHandWidth(hRect.width);
        }
      }
    };
    update();
    const obs = new ResizeObserver(update);
    if (boardEl) obs.observe(boardEl);
    if (handEl) obs.observe(handEl);
    return () => obs.disconnect();
  }, []);

  const displayedPlayer = currentPlayer.isAI ? match.players[0] : currentPlayer;
  const handLayout = useMemo(
    () =>
      computeHandLayout(
        displayedPlayer.hand.length,
        Math.max(180, handWidth - 8),
        48,
        96,
        8
      ),
    [displayedPlayer.hand.length, handWidth]
  );

  const layout = useMemo(
    () => computeBoardLayout(match.board, match.mode),
    [match.board, match.mode]
  );

  const openEnds = useMemo(
    () => getOpenEnds(match.board, match.mode),
    [match.board, match.mode]
  );

  const openEndsSum = useMemo(
    () => getOpenEndsSum(match.board, match.mode),
    [match.board, match.mode]
  );

  const legalMoves = useMemo(
    () =>
      isHumanTurn
        ? getLegalMoves(currentPlayer.hand, match.board, match.mode)
        : [],
    [isHumanTurn, currentPlayer.hand, match.board, match.mode]
  );

  const playableTileIds = useMemo(
    () => new Set(legalMoves.map((m) => m.tile.id)),
    [legalMoves]
  );

  const activeTile = useMemo(() => {
    const id = drag?.active ? drag.tile.id : selectedTileId;
    if (!id) return null;
    return currentPlayer.hand.find((t) => t.id === id) ?? null;
  }, [drag, selectedTileId, currentPlayer.hand]);

  const validBranchesForActiveTile = useMemo(() => {
    if (!activeTile || !isHumanTurn) return new Set<'root' | BranchDir>();
    if (!match.board.root) return new Set<'root' | BranchDir>(['root']);
    const set = new Set<'root' | BranchDir>();
    openEnds.forEach((end) => {
      if (tileCanMatchEnd(activeTile, end)) {
        set.add(end.branch);
      }
    });
    return set;
  }, [activeTile, isHumanTurn, match.board.root, openEnds]);

  // Compute responsive scale so the spinner stays dead-center (when active) and all tiles remain visible
  const scale = useMemo(() => {
    const padX = 24;
    const padY = 24;
    const availW = Math.max(200, viewport.width - padX);
    const availH = Math.max(160, viewport.height - padY);

    if (layout.hasSpinner) {
      const sx = (availW / 2) / layout.bounds.halfSpanX;
      const sy = (availH / 2) / layout.bounds.halfSpanY;
      return Math.min(1.08, Math.max(0.28, Math.min(sx, sy)));
    }

    const sx = availW / layout.bounds.width;
    const sy = availH / layout.bounds.height;
    return Math.min(1.12, Math.max(0.32, Math.min(sx, sy)));
  }, [viewport, layout]);

  const centerOffsetX = layout.hasSpinner
    ? 0
    : -(layout.bounds.minX + layout.bounds.width / 2);
  const centerOffsetY = layout.hasSpinner
    ? 0
    : -(layout.bounds.minY + layout.bounds.height / 2);

  // Pointer drag-and-drop handlers
  const findNearestValidDrop = (
    clientX: number,
    clientY: number,
    tile: DominoTile
  ): 'root' | BranchDir | null => {
    const valid = new Set<'root' | BranchDir>();
    if (!match.board.root) {
      valid.add('root');
    } else {
      openEnds.forEach((end) => {
        if (tileCanMatchEnd(tile, end)) valid.add(end.branch);
      });
    }

    let bestBranch: 'root' | BranchDir | null = null;
    let bestDist = 72; // generous touch snap radius in px

    valid.forEach((branch) => {
      const el = dropRefs.current.get(branch);
      if (!el) return;
      const rect = el.getBoundingClientRect();
      const cx = rect.left + rect.width / 2;
      const cy = rect.top + rect.height / 2;
      const dist = Math.hypot(clientX - cx, clientY - cy);
      if (dist < bestDist) {
        bestDist = dist;
        bestBranch = branch;
      }
    });

    return bestBranch;
  };

  const handleTilePointerDown = (
    e: React.PointerEvent<HTMLDivElement>,
    tile: DominoTile
  ) => {
    if (!isHumanTurn || !playableTileIds.has(tile.id)) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    onSelectTileHaptic();
    setDrag({
      tile,
      startX: e.clientX,
      startY: e.clientY,
      currentX: e.clientX,
      currentY: e.clientY,
      active: false,
    });
  };

  const handleTilePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!drag) return;
    const dist = Math.hypot(e.clientX - drag.startX, e.clientY - drag.startY);
    const isNowActive = drag.active || dist > 9;
    if (isNowActive) {
      const nearest = findNearestValidDrop(e.clientX, e.clientY, drag.tile);
      setHoveredBranch(nearest);
    }
    setDrag({
      ...drag,
      currentX: e.clientX,
      currentY: e.clientY,
      active: isNowActive,
    });
  };

  const handleTilePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!drag) return;
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId);
    }

    if (drag.active) {
      const target = findNearestValidDrop(e.clientX, e.clientY, drag.tile);
      if (target) {
        onPlayTile(drag.tile.id, target);
      }
      setDrag(null);
      setHoveredBranch(null);
      return;
    }

    // Was a clean tap!
    const tappedTile = drag.tile;
    setDrag(null);
    setHoveredBranch(null);

    const matchingBranches: ('root' | BranchDir)[] = !match.board.root
      ? ['root']
      : openEnds.filter((end) => tileCanMatchEnd(tappedTile, end)).map((end) => end.branch);

    if (selectedTileId === tappedTile.id && matchingBranches.length === 1) {
      // Second tap on already-selected tile with 1 valid end auto-places it!
      onPlayTile(tappedTile.id, matchingBranches[0]);
      setSelectedTileId(null);
    } else if (matchingBranches.length === 1 && !match.board.root) {
      // Empty board first tile: tap to select, tap again or tap center
      setSelectedTileId(tappedTile.id);
    } else {
      setSelectedTileId((prev) => (prev === tappedTile.id ? null : tappedTile.id));
    }
  };

  const mustDraw =
    isHumanTurn &&
    legalMoves.length === 0 &&
    modeMeta.hasBoneyard &&
    match.boneyard.length > 0;

  const mustPass =
    isHumanTurn &&
    legalMoves.length === 0 &&
    (!modeMeta.hasBoneyard || match.boneyard.length === 0);

  return (
    <div className="flex flex-col h-full w-full max-w-5xl mx-auto px-3 sm:px-5 pt-2 pb-2 gap-2.5 overflow-hidden">
      {/* Top Compact Status & Actions Strip */}
      <div className="flex items-center justify-between gap-2 shrink-0">
        <div className="flex items-center gap-2 min-w-0 text-xs text-[#5A606D] dark:text-[#9AA5B8]">
          <span className="font-display font-semibold text-sm text-[#171A21] dark:text-[#F5F2EB] truncate">
            {modeMeta.name}
          </span>
          <span aria-hidden="true">·</span>
          <span className="whitespace-nowrap">Round {match.roundNumber}</span>
          {match.board.root && !isRoundBasedMode(match.mode) && (
            <>
              <span aria-hidden="true">·</span>
              <span className="font-mono-num whitespace-nowrap font-medium text-[#171A21] dark:text-[#F5F2EB]">
                Open {openEndsSum}
              </span>
            </>
          )}
          {modeMeta.hasBoneyard && (
            <>
              <span aria-hidden="true" className="hidden sm:inline">·</span>
              <span className="hidden sm:inline font-mono-num whitespace-nowrap">
                Boneyard {match.boneyard.length}
              </span>
            </>
          )}
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            onClick={onOpenRuleTip}
            aria-label="Mode rules"
            className="min-h-[38px] min-w-[38px] px-2.5 rounded-xl flex items-center justify-center gap-1.5 text-xs font-medium bg-[#F2EEE6] dark:bg-[#161E2E] text-[#171A21] dark:text-[#F5F2EB] hover:bg-[#EAE4D8] dark:hover:bg-[#1E283C] transition-colors whitespace-nowrap"
          >
            <HelpCircle className="w-4 h-4 text-teal-700 dark:text-teal-400" />
            <span className="hidden sm:inline">Rules</span>
          </button>

          <button
            type="button"
            onClick={onRequestQuit}
            aria-label="Quit game"
            className="min-h-[38px] min-w-[38px] rounded-xl flex items-center justify-center bg-[#F2EEE6] dark:bg-[#161E2E] text-[#5A606D] dark:text-[#9AA5B8] hover:text-rose-600 dark:hover:text-rose-400 hover:bg-[#EAE4D8] dark:hover:bg-[#1E283C] transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Compact Player Row & Target Progress */}
      <div className="shrink-0">
        <PlayerScoreBar
          mode={match.mode}
          players={match.players}
          currentPlayerIndex={match.currentPlayerIndex}
          targetScore={match.targetScore}
          isAIThinking={isAIThinking}
          lastDrawnTileId={match.lastDrawnTileId}
          lastDrawnPlayerId={match.lastDrawnPlayerId}
          roundWinnerId={roundWinCelebration?.winnerId ?? null}
        />
      </div>

      {/* Center Tabletop Game Board */}
      <div
        ref={boardRef}
        className="relative flex-1 min-h-0 rounded-3xl overflow-hidden bg-[#ECE6DA] dark:bg-[#131A27] border border-black/5 dark:border-white/5 shadow-inner"
      >
        {/* Subtle tabletop felt grain radial vignette */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_45%,rgba(0,0,0,0.05)_100%)] dark:bg-[radial-gradient(circle_at_center,rgba(45,212,191,0.03)_0%,rgba(0,0,0,0.32)_100%)]"
        />

        {/* Quiet Action Toast inside board top */}
        <div className="pointer-events-none absolute top-2.5 inset-x-3 z-20 flex items-center justify-between gap-2">
          {match.actionBanner && (
            <span className="text-[11px] font-medium text-[#5A606D] dark:text-[#9AA5B8] bg-[#FAF8F4]/85 dark:bg-[#0E131D]/80 backdrop-blur-sm px-2.5 py-1 rounded-lg truncate max-w-[75%]">
              {match.actionBanner}
            </span>
          )}
          {modeMeta.hasBoneyard && (
            <span className="sm:hidden ml-auto text-[11px] font-mono-num font-medium text-[#5A606D] dark:text-[#9AA5B8] bg-[#FAF8F4]/85 dark:bg-[#0E131D]/80 backdrop-blur-sm px-2 py-1 rounded-lg flex items-center gap-1">
              <Layers className="w-3 h-3" />
              {match.boneyard.length}
            </span>
          )}
        </div>

        {/* Round Win Celebratory Overlay Animation */}
        <AnimatePresence>
          {roundWinCelebration && (
            <motion.div
              key={`round-win-${roundWinCelebration.roundNumber}-${roundWinCelebration.winnerId}`}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.25 }}
              className="pointer-events-none absolute inset-0 z-30 flex items-center justify-center bg-amber-500/10 dark:bg-amber-400/10 backdrop-blur-[1px]"
            >
              <div className="relative flex flex-col items-center">
                {SPARKLE_ANGLES.map((deg) => (
                  <motion.span
                    key={deg}
                    initial={{ x: 0, y: 0, opacity: 1, scale: 1.1 }}
                    animate={{
                      x: Math.cos((deg * Math.PI) / 180) * 86,
                      y: Math.sin((deg * Math.PI) / 180) * 56,
                      opacity: 0,
                      scale: 0.25,
                    }}
                    transition={{ duration: 1.05, ease: 'easeOut' }}
                    className="absolute top-1/2 left-1/2 w-2.5 h-2.5 rounded-full bg-amber-400 dark:bg-amber-300"
                  />
                ))}
                <motion.div
                  initial={{ scale: 0.72, y: 12, opacity: 0 }}
                  animate={{ scale: 1, y: 0, opacity: 1 }}
                  exit={{ scale: 0.92, y: -12, opacity: 0 }}
                  transition={{ type: 'spring', stiffness: 340, damping: 22 }}
                  className="px-6 py-3 rounded-3xl bg-[#171A21]/92 dark:bg-[#FAF8F4]/95 text-[#FAF8F4] dark:text-[#171A21] shadow-2xl border border-amber-400/50 text-center"
                >
                  <p className="text-[11px] font-semibold tracking-wider uppercase text-amber-400 dark:text-teal-700">
                    Round {roundWinCelebration.roundNumber} Complete
                  </p>
                  <p className="font-display text-xl font-bold mt-0.5">
                    {roundWinCelebration.winnerName} Wins the Round!
                  </p>
                </motion.div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Scoring Glow & Particle Burst */}
        <AnimatePresence>
          {activeScoreBanner && !roundWinCelebration && (
            <motion.div
              key={activeScoreBanner.id}
              initial={{ opacity: 0, scale: 0.75, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: -18 }}
              transition={{ duration: 0.25 }}
              className="pointer-events-none absolute inset-x-0 top-12 z-30 flex flex-col items-center"
            >
              <div className="relative flex items-center justify-center">
                {/* Particle sparkles */}
                {SPARKLE_ANGLES.map((deg) => (
                  <motion.span
                    key={deg}
                    initial={{ x: 0, y: 0, opacity: 1, scale: 1 }}
                    animate={{
                      x: Math.cos((deg * Math.PI) / 180) * 52,
                      y: Math.sin((deg * Math.PI) / 180) * 32,
                      opacity: 0,
                      scale: 0.3,
                    }}
                    transition={{ duration: 0.75, ease: 'easeOut' }}
                    className="absolute w-2 h-2 rounded-full bg-amber-400 dark:bg-amber-300"
                  />
                ))}
                <div className="px-4 py-1.5 rounded-2xl bg-amber-500 text-slate-950 font-display font-bold text-base shadow-lg shadow-amber-500/30">
                  +{activeScoreBanner.points} pts · {activeScoreBanner.reason}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Scaled & Centered Board Coordinate Space */}
        <div className="absolute inset-0 flex items-center justify-center">
          <motion.div
            animate={{ scale }}
            transition={{ type: 'spring', stiffness: 200, damping: 28 }}
            style={{ transformOrigin: 'center center' }}
            className="relative w-0 h-0 flex items-center justify-center"
          >
            {/* Empty Board Center Drop Slot */}
            {!match.board.root && (
              <div className="flex flex-col items-center justify-center shrink-0">
                <button
                  ref={(el) => {
                    if (el) dropRefs.current.set('root', el);
                    else dropRefs.current.delete('root');
                  }}
                  type="button"
                  disabled={!activeTile || !isHumanTurn}
                  onClick={() => {
                    if (activeTile && isHumanTurn) {
                      onPlayTile(activeTile.id, 'root');
                    }
                  }}
                  className={[
                    'w-[100px] h-[54px] rounded-xl border-2 border-dashed flex items-center justify-center text-xs font-semibold transition-all',
                    activeTile && isHumanTurn
                      ? hoveredBranch === 'root'
                        ? 'border-teal-600 bg-teal-600/20 text-teal-800 dark:border-teal-400 dark:bg-teal-400/20 dark:text-teal-200 scale-110'
                        : 'border-teal-600/70 bg-teal-600/10 text-teal-800 dark:border-teal-400/70 dark:bg-teal-400/10 dark:text-teal-200 animate-pulse cursor-pointer'
                      : 'border-black/15 dark:border-white/15 text-[#5A606D] dark:text-[#9AA5B8]',
                  ].join(' ')}
                >
                  {isHumanTurn
                    ? activeTile
                      ? 'Place First Tile'
                      : 'Select a Tile'
                    : `${currentPlayer.name} Opening...`}
                </button>
              </div>
            )}

            {/* Placed Domino Tiles */}
            {layout.tiles.map((item) => {
              const isNew = item.placed.tile.id === match.lastPlayedTileId;
              const isScoringGlow = Boolean(activeScoreBanner && isNew);
              const leftPx = item.x + centerOffsetX - item.width / 2;
              const topPx = item.y + centerOffsetY - item.height / 2;

              return (
                <motion.div
                  key={item.placed.tile.id}
                  initial={
                    isNew
                      ? { scale: 0.68, opacity: 0, left: leftPx, top: topPx }
                      : false
                  }
                  animate={{ scale: 1, opacity: 1, left: leftPx, top: topPx }}
                  transition={{ type: 'spring', stiffness: 360, damping: 28 }}
                  style={{
                    position: 'absolute',
                    width: `${item.width}px`,
                    height: `${item.height}px`,
                  }}
                >
                  {isScoringGlow && (
                    <div
                      aria-hidden="true"
                      className="absolute -inset-2 rounded-2xl bg-amber-400/35 blur-md pointer-events-none"
                    />
                  )}
                  <DominoTileView
                    tile={item.placed.tile}
                    orientation={item.orientation}
                    firstPip={item.firstPip}
                    secondPip={item.secondPip}
                    width={item.width}
                    height={item.height}
                    isSpinner={item.placed.isSpinner}
                    isNew={isNew}
                  />
                </motion.div>
              );
            })}

            {/* Interactive Drop Zones on Open Ends */}
            {isHumanTurn &&
              activeTile &&
              layout.dropZones.map((dz) => {
                const isValid = validBranchesForActiveTile.has(dz.end.branch);
                if (!isValid) return null;
                const isHovered = hoveredBranch === dz.end.branch;

                return (
                  <button
                    key={dz.end.branch}
                    ref={(el) => {
                      if (el) dropRefs.current.set(dz.end.branch, el);
                      else dropRefs.current.delete(dz.end.branch);
                    }}
                    type="button"
                    aria-label={`Place on ${dz.end.branch} end matching ${dz.end.requiredPip}`}
                    onClick={() => onPlayTile(activeTile.id, dz.end.branch)}
                    style={{
                      position: 'absolute',
                      left: `${dz.x + centerOffsetX - dz.width / 2}px`,
                      top: `${dz.y + centerOffsetY - dz.height / 2}px`,
                      width: `${dz.width}px`,
                      height: `${dz.height}px`,
                    }}
                    className={[
                      'rounded-[10px] border-2 border-dashed flex items-center justify-center font-mono-num text-sm font-bold transition-all cursor-pointer z-20',
                      isHovered
                        ? 'bg-teal-600/30 border-teal-600 text-teal-900 dark:bg-teal-400/30 dark:border-teal-300 dark:text-teal-100 scale-110 shadow-md'
                        : 'bg-teal-600/12 border-teal-600/80 text-teal-800 dark:bg-teal-400/15 dark:border-teal-400/80 dark:text-teal-200 hover:bg-teal-600/25',
                    ].join(' ')}
                  >
                    {dz.end.requiredPip}
                  </button>
                );
              })}
          </motion.div>
        </div>
      </div>

      {/* Bottom Ergonomic Player Hand & Action Dock */}
      <div className="shrink-0 rounded-3xl bg-[#F2EEE6] dark:bg-[#161E2E] border border-black/5 dark:border-white/5 px-3 py-2.5">
        <div className="flex items-center justify-between gap-2 mb-2 px-1">
          <span className="text-xs font-medium text-[#5A606D] dark:text-[#9AA5B8]">
            {isHumanTurn
              ? selectedTileId
                ? 'Tap a highlighted board end to place'
                : legalMoves.length > 0
                ? `${currentPlayer.name}’s hand · Drag or tap a tile`
                : mustDraw
                ? 'No match — draw from boneyard'
                : 'No playable tiles — pass turn'
              : `${currentPlayer.name} is playing…`}
          </span>

          {mustDraw && (
            <button
              type="button"
              onClick={onDrawTile}
              className="min-h-[40px] px-4 rounded-xl bg-teal-700 hover:bg-teal-800 dark:bg-teal-500 dark:hover:bg-teal-400 text-white dark:text-slate-950 text-xs font-semibold flex items-center gap-1.5 shadow-sm transition-colors whitespace-nowrap"
            >
              <Layers className="w-3.5 h-3.5" />
              Draw ({match.boneyard.length})
            </button>
          )}

          {mustPass && (
            <button
              type="button"
              onClick={onPassTurn}
              className="min-h-[40px] px-4 rounded-xl bg-amber-600 hover:bg-amber-700 dark:bg-amber-400 dark:hover:bg-amber-300 text-white dark:text-slate-950 text-xs font-semibold flex items-center gap-1.5 shadow-sm transition-colors whitespace-nowrap"
            >
              <SkipForward className="w-3.5 h-3.5" />
              Pass Turn
            </button>
          )}
        </div>

        {/* Auto-Scaled Tile Hand Tray — all tiles fit without overlap or scroll */}
        <div
          ref={handRef}
          style={{ gap: `${handLayout.gap}px` }}
          className="w-full min-h-[108px] flex items-center justify-center py-1.5 px-1 overflow-visible"
        >
          <AnimatePresence initial={false}>
            {displayedPlayer.hand.map((tile) => {
              const playable = isHumanTurn && playableTileIds.has(tile.id);
              const isBeingDragged = drag?.active && drag.tile.id === tile.id;
              const isNewDraw =
                displayedPlayer.id === match.lastDrawnPlayerId &&
                tile.id === match.lastDrawnTileId;

              return (
                <motion.div
                  key={tile.id}
                  layout
                  initial={
                    isNewDraw
                      ? { y: -88, opacity: 0, scale: 0.72 }
                      : { opacity: 0, scale: 0.8 }
                  }
                  animate={{
                    y: 0,
                    opacity: isBeingDragged ? 0.25 : 1,
                    scale: 1,
                    width: handLayout.tileWidth,
                    height: handLayout.tileHeight,
                  }}
                  exit={{ opacity: 0, scale: 0.7, y: -24 }}
                  transition={{ type: 'spring', stiffness: 360, damping: 26 }}
                  className="shrink-0 flex items-center justify-center"
                >
                  <DominoTileView
                    tile={tile}
                    width={handLayout.tileWidth}
                    height={handLayout.tileHeight}
                    isPlayable={playable}
                    isSelected={selectedTileId === tile.id}
                    isDimmed={currentPlayer.isAI || (isHumanTurn && !playable)}
                    onPointerDown={(e) => handleTilePointerDown(e, tile)}
                    onPointerMove={handleTilePointerMove}
                    onPointerUp={handleTilePointerUp}
                    onPointerCancel={handleTilePointerUp}
                    onClick={(e) => {
                      if (e.detail === 0 && playable) {
                        onSelectTileHaptic();
                        setSelectedTileId((prev) =>
                          prev === tile.id ? null : tile.id
                        );
                      }
                    }}
                    className="touch-none"
                  />
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      </div>

      {/* Floating 60fps Drag Ghost */}
      {drag && drag.active && (
        <div
          style={{
            transform: `translate3d(${drag.currentX - 24}px, ${drag.currentY - 50}px, 0) scale(1.08) rotate(-2deg)`,
          }}
          className="pointer-events-none fixed top-0 left-0 z-50 drop-shadow-2xl"
        >
          <DominoTileView tile={drag.tile} size="hand" isSelected />
        </div>
      )}
    </div>
  );
};
