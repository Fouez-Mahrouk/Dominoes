import {
  BoardBranches,
  BranchDir,
  Difficulty,
  DominoTile,
  GameMode,
  MatchState,
  OpenEnd,
  PlacedTile,
  Player,
  PlayerCount,
  RoundSummary,
  TargetScore,
} from '../types/domino';

export interface ModeMeta {
  id: GameMode;
  name: string;
  subtitle: string;
  defaultTarget: TargetScore;
  hasBoneyard: boolean;
  hasSpinner: boolean;
  shortRule: string;
}

export function isRoundBasedMode(mode: GameMode): boolean {
  return mode === 'free_game' || mode === 'block';
}

export const MODE_META: Record<GameMode, ModeMeta> = {
  free_game: {
    id: 'free_game',
    name: 'Free Game',
    subtitle: 'Open dominoes · Draw & empty hand',
    defaultTarget: 3,
    hasBoneyard: true,
    hasSpinner: false,
    shortRule: '7 tiles each. Draw from boneyard when stuck. First to empty hand or lowest pips when blocked wins 1 round.',
  },
  block: {
    id: 'block',
    name: 'Block',
    subtitle: 'Classic tactical dominoes',
    defaultTarget: 3,
    hasBoneyard: false,
    hasSpinner: false,
    shortRule: 'No drawing. Match open ends or pass. First to empty hand or lowest pips when blocked wins 1 round.',
  },
  all_fives: {
    id: 'all_fives',
    name: 'All Fives',
    subtitle: 'Muggins · Multiples of 5',
    defaultTarget: 100,
    hasBoneyard: true,
    hasSpinner: true,
    shortRule: 'Draw when stuck. Score the sum whenever open ends total a multiple of 5. First double is a 4-way spinner.',
  },
  all_threes: {
    id: 'all_threes',
    name: 'All Threes',
    subtitle: 'Fast scoring · Multiples of 3',
    defaultTarget: 100,
    hasBoneyard: true,
    hasSpinner: true,
    shortRule: 'Draw when stuck. Score the sum whenever open ends total a multiple of 3. First double is a 4-way spinner.',
  },
  fives_and_threes: {
    id: 'fives_and_threes',
    name: 'Fives & Threes',
    subtitle: '1 pt per 5 or 3',
    defaultTarget: 100,
    hasBoneyard: true,
    hasSpinner: true,
    shortRule: 'Score 1 point per 5 and 1 point per 3 whenever open ends divide by 5 or 3 (e.g. 15 = 8 pts).',
  },
};

export const AI_NAMES = ['Maya', 'Theo', 'Nadia', 'Liam'];

export function createDoubleSixSet(): DominoTile[] {
  const deck: DominoTile[] = [];
  for (let a = 0; a <= 6; a++) {
    for (let b = a; b <= 6; b++) {
      deck.push({ id: `${a}-${b}`, a, b });
    }
  }
  return deck;
}

export function shuffleTiles(tiles: DominoTile[]): DominoTile[] {
  const arr = [...tiles];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

export function sumPips(hand: DominoTile[]): number {
  return hand.reduce((acc, t) => acc + t.a + t.b, 0);
}

export function createEmptyBoard(): BoardBranches {
  return {
    root: null,
    left: [],
    right: [],
    up: [],
    down: [],
    spinnerTileId: null,
    spinnerLocation: null,
  };
}

function getHandSize(mode: GameMode, playerCount: PlayerCount): number {
  if (mode === 'block' || mode === 'free_game') {
    return 7;
  }
  // Scoring draw modes guarantee an active boneyard across 2, 3, or 4 players
  if (playerCount === 2) return 7;
  if (playerCount === 3) return 6;
  return 5;
}

function findStartingPlayerIndex(players: Player[]): number {
  // Look for highest double from 6-6 down to 0-0
  for (let pip = 6; pip >= 0; pip--) {
    for (let i = 0; i < players.length; i++) {
      if (players[i].hand.some((t) => t.a === pip && t.b === pip)) {
        return i;
      }
    }
  }
  // Fallback: player with highest total pip single tile
  let bestIdx = 0;
  let bestVal = -1;
  players.forEach((p, idx) => {
    p.hand.forEach((t) => {
      if (t.a + t.b > bestVal) {
        bestVal = t.a + t.b;
        bestIdx = idx;
      }
    });
  });
  return bestIdx;
}

export function createNewMatch(params: {
  mode: GameMode;
  playerCount: PlayerCount;
  passAndPlay: boolean;
  targetScore: TargetScore;
  difficulty: Difficulty;
}): MatchState {
  const { mode, playerCount, passAndPlay, targetScore, difficulty } = params;
  const players: Player[] = [];

  for (let i = 0; i < playerCount; i++) {
    const isHuman = i === 0 || (passAndPlay && playerCount === 4 && i === 2);
    const name = isHuman
      ? i === 0
        ? 'You'
        : 'Player 3'
      : AI_NAMES[i - 1] || `Bot ${i}`;
    players.push({
      id: i,
      name,
      isAI: !isHuman,
      avatarIndex: i,
      hand: [],
      score: 0,
      roundsWon: 0,
    });
  }

  return dealRound({
    phase: 'PLAYING',
    mode,
    playerCount,
    passAndPlay,
    targetScore,
    difficulty,
    roundNumber: 1,
    players,
    currentPlayerIndex: 0,
    boneyard: [],
    board: createEmptyBoard(),
    consecutivePasses: 0,
    moveSequence: 0,
    lastPlayedTileId: null,
    lastDrawnTileId: null,
    lastDrawnPlayerId: null,
    lastScoringEvent: null,
    roundSummary: null,
    matchWinnerId: null,
    actionBanner: null,
  }, null);
}

export function dealRound(prevState: MatchState, roundStarterId: number | null): MatchState {
  const deck = shuffleTiles(createDoubleSixSet());
  const handSize = getHandSize(prevState.mode, prevState.playerCount);
  let cursor = 0;

  const players = prevState.players.map((p) => {
    const hand = deck.slice(cursor, cursor + handSize);
    cursor += handSize;
    return { ...p, hand };
  });

  const boneyard = deck.slice(cursor);
  const starterIdx =
    roundStarterId !== null
      ? Math.max(0, players.findIndex((p) => p.id === roundStarterId))
      : findStartingPlayerIndex(players);

  const starter = players[starterIdx];
  const needsPassScreen = prevState.passAndPlay && !starter.isAI && prevState.roundNumber > 1;

  return {
    ...prevState,
    phase: needsPassScreen ? 'PASS_DEVICE' : 'PLAYING',
    players,
    boneyard,
    board: createEmptyBoard(),
    currentPlayerIndex: starterIdx,
    consecutivePasses: 0,
    moveSequence: 0,
    lastPlayedTileId: null,
    lastDrawnTileId: null,
    lastDrawnPlayerId: null,
    lastScoringEvent: null,
    roundSummary: null,
    actionBanner:
      prevState.roundNumber === 1
        ? `${starter.name} opens Round 1`
        : `${starter.name} leads Round ${prevState.roundNumber}`,
  };
}

export function findSpinnerTile(board: BoardBranches): PlacedTile | null {
  if (!board.spinnerTileId) return null;
  if (board.root && board.root.tile.id === board.spinnerTileId) return board.root;
  const inLeft = board.left.find((t) => t.tile.id === board.spinnerTileId);
  if (inLeft) return inLeft;
  const inRight = board.right.find((t) => t.tile.id === board.spinnerTileId);
  if (inRight) return inRight;
  return null;
}

export function isSpinnerCrossed(board: BoardBranches): boolean {
  if (!board.spinnerTileId || !board.spinnerLocation) return false;
  if (board.spinnerLocation === 'root') {
    return board.left.length > 0 && board.right.length > 0;
  }
  if (board.spinnerLocation === 'left') {
    const idx = board.left.findIndex((t) => t.tile.id === board.spinnerTileId);
    return idx >= 0 && board.left.length - 1 > idx;
  }
  if (board.spinnerLocation === 'right') {
    const idx = board.right.findIndex((t) => t.tile.id === board.spinnerTileId);
    return idx >= 0 && board.right.length - 1 > idx;
  }
  return false;
}

export function getOpenEnds(board: BoardBranches, mode: GameMode): OpenEnd[] {
  if (!board.root) return [];

  const ends: OpenEnd[] = [];
  const root = board.root;
  const singleTileOnly =
    board.left.length === 0 &&
    board.right.length === 0 &&
    board.up.length === 0 &&
    board.down.length === 0;

  if (singleTileOnly) {
    ends.push({
      branch: 'left',
      requiredPip: root.innerPip,
      scoreValue: root.isDouble ? root.innerPip : root.innerPip,
      isDoubleTip: root.isDouble,
      anchorTileId: root.tile.id,
    });
    ends.push({
      branch: 'right',
      requiredPip: root.outerPip,
      scoreValue: root.isDouble ? root.outerPip : root.outerPip,
      isDoubleTip: root.isDouble,
      anchorTileId: root.tile.id,
    });
    return ends;
  }

  // Left end
  if (board.left.length > 0) {
    const tip = board.left[board.left.length - 1];
    ends.push({
      branch: 'left',
      requiredPip: tip.outerPip,
      scoreValue: tip.isDouble ? tip.outerPip * 2 : tip.outerPip,
      isDoubleTip: tip.isDouble,
      anchorTileId: tip.tile.id,
    });
  } else {
    ends.push({
      branch: 'left',
      requiredPip: root.innerPip,
      scoreValue: root.isDouble ? root.innerPip * 2 : root.innerPip,
      isDoubleTip: root.isDouble,
      anchorTileId: root.tile.id,
    });
  }

  // Right end
  if (board.right.length > 0) {
    const tip = board.right[board.right.length - 1];
    ends.push({
      branch: 'right',
      requiredPip: tip.outerPip,
      scoreValue: tip.isDouble ? tip.outerPip * 2 : tip.outerPip,
      isDoubleTip: tip.isDouble,
      anchorTileId: tip.tile.id,
    });
  } else {
    ends.push({
      branch: 'right',
      requiredPip: root.outerPip,
      scoreValue: root.isDouble ? root.outerPip * 2 : root.outerPip,
      isDoubleTip: root.isDouble,
      anchorTileId: root.tile.id,
    });
  }

  // Up and Down spinner branches
  if (MODE_META[mode].hasSpinner && isSpinnerCrossed(board)) {
    const spinner = findSpinnerTile(board);
    if (spinner) {
      if (board.up.length > 0) {
        const tip = board.up[board.up.length - 1];
        ends.push({
          branch: 'up',
          requiredPip: tip.outerPip,
          scoreValue: tip.isDouble ? tip.outerPip * 2 : tip.outerPip,
          isDoubleTip: tip.isDouble,
          anchorTileId: tip.tile.id,
        });
      } else {
        ends.push({
          branch: 'up',
          requiredPip: spinner.tile.a,
          scoreValue: 0,
          isDoubleTip: false,
          anchorTileId: spinner.tile.id,
        });
      }

      if (board.down.length > 0) {
        const tip = board.down[board.down.length - 1];
        ends.push({
          branch: 'down',
          requiredPip: tip.outerPip,
          scoreValue: tip.isDouble ? tip.outerPip * 2 : tip.outerPip,
          isDoubleTip: tip.isDouble,
          anchorTileId: tip.tile.id,
        });
      } else {
        ends.push({
          branch: 'down',
          requiredPip: spinner.tile.a,
          scoreValue: 0,
          isDoubleTip: false,
          anchorTileId: spinner.tile.id,
        });
      }
    }
  }

  return ends;
}

export function getOpenEndsSum(board: BoardBranches, mode: GameMode): number {
  if (!board.root) return 0;
  const ends = getOpenEnds(board, mode);
  return ends.reduce((sum, end) => sum + end.scoreValue, 0);
}

export function calculatePlayScore(
  openSum: number,
  mode: GameMode
): { points: number; reason: string } {
  if (openSum <= 0) return { points: 0, reason: '' };

  if (mode === 'all_fives') {
    if (openSum % 5 === 0) {
      return { points: openSum, reason: `Multiple of 5 (${openSum})` };
    }
    return { points: 0, reason: '' };
  }

  if (mode === 'all_threes') {
    if (openSum % 3 === 0) {
      return { points: openSum, reason: `Multiple of 3 (${openSum})` };
    }
    return { points: 0, reason: '' };
  }

  if (mode === 'fives_and_threes') {
    const fives = openSum % 5 === 0 ? openSum / 5 : 0;
    const threes = openSum % 3 === 0 ? openSum / 3 : 0;
    const points = fives + threes;
    if (points > 0) {
      const parts: string[] = [];
      if (fives > 0) parts.push(`${fives}×5`);
      if (threes > 0) parts.push(`${threes}×3`);
      return { points, reason: `Open ${openSum} (${parts.join(' + ')})` };
    }
    return { points: 0, reason: '' };
  }

  return { points: 0, reason: '' };
}

export interface LegalMove {
  tile: DominoTile;
  branch: 'root' | BranchDir;
}

export function getLegalMoves(
  hand: DominoTile[],
  board: BoardBranches,
  mode: GameMode
): LegalMove[] {
  if (!board.root) {
    return hand.map((tile) => ({ tile, branch: 'root' }));
  }

  const openEnds = getOpenEnds(board, mode);
  const moves: LegalMove[] = [];

  for (const tile of hand) {
    for (const end of openEnds) {
      if (tile.a === end.requiredPip || tile.b === end.requiredPip) {
        moves.push({ tile, branch: end.branch });
      }
    }
  }
  return moves;
}

export function tileCanMatchEnd(tile: DominoTile, end: OpenEnd): boolean {
  return tile.a === end.requiredPip || tile.b === end.requiredPip;
}

function placeTileOnBoard(
  board: BoardBranches,
  tile: DominoTile,
  branch: 'root' | BranchDir,
  playerId: number,
  sequence: number,
  mode: GameMode
): BoardBranches {
  const isDouble = tile.a === tile.b;
  const canBecomeSpinner = MODE_META[mode].hasSpinner && isDouble && board.spinnerTileId === null;

  if (branch === 'root' || !board.root) {
    const placed: PlacedTile = {
      tile,
      branch: 'root',
      innerPip: tile.a,
      outerPip: tile.b,
      isDouble,
      isSpinner: canBecomeSpinner,
      playedBy: playerId,
      sequence,
    };
    return {
      ...board,
      root: placed,
      spinnerTileId: canBecomeSpinner ? tile.id : board.spinnerTileId,
      spinnerLocation: canBecomeSpinner ? 'root' : board.spinnerLocation,
    };
  }

  const openEnds = getOpenEnds(board, mode);
  const targetEnd = openEnds.find((e) => e.branch === branch);
  if (!targetEnd) return board;

  const req = targetEnd.requiredPip;
  const innerPip = req;
  const outerPip = tile.a === req ? tile.b : tile.a;

  const placed: PlacedTile = {
    tile,
    branch,
    innerPip,
    outerPip,
    isDouble,
    isSpinner: canBecomeSpinner,
    playedBy: playerId,
    sequence,
  };

  const nextBoard: BoardBranches = {
    ...board,
    [branch]: [...board[branch], placed],
    spinnerTileId: canBecomeSpinner ? tile.id : board.spinnerTileId,
    spinnerLocation:
      canBecomeSpinner && (branch === 'left' || branch === 'right')
        ? branch
        : board.spinnerLocation,
  };

  return nextBoard;
}

function finalizeRound(
  state: MatchState,
  winType: 'domino' | 'blocked',
  dominoWinnerId: number | null
): MatchState {
  const players = state.players.map((p) => ({ ...p }));
  let winnerId: number | null = dominoWinnerId;
  let finalWinType: 'domino' | 'blocked' | 'tie' = winType;

  if (winType === 'blocked') {
    const counts = players.map((p) => ({ id: p.id, pips: sumPips(p.hand) }));
    const minPips = Math.min(...counts.map((c) => c.pips));
    const tied = counts.filter((c) => c.pips === minPips);
    if (tied.length === 1) {
      winnerId = tied[0].id;
    } else {
      winnerId = null;
      finalWinType = 'tie';
    }
  }

  let pointsAwarded = 0;
  const roundBased = isRoundBasedMode(state.mode);
  if (winnerId !== null) {
    const winner = players.find((p) => p.id === winnerId)!;
    const opponentsPips = players
      .filter((p) => p.id !== winnerId)
      .reduce((acc, p) => acc + sumPips(p.hand), 0);
    const winnerPips = sumPips(winner.hand);
    const rawDiff = Math.max(0, opponentsPips - winnerPips);

    if (roundBased) {
      pointsAwarded = 0;
    } else if (state.mode === 'all_fives') {
      pointsAwarded = Math.round(rawDiff / 5) * 5;
    } else if (state.mode === 'all_threes') {
      pointsAwarded = Math.round(rawDiff / 3) * 3;
    } else if (state.mode === 'fives_and_threes') {
      pointsAwarded = (winType === 'domino' ? 1 : 0) + Math.floor(rawDiff / 5);
    }

    winner.score += pointsAwarded;
    winner.roundsWon += 1;
  }

  // Check if anyone reached target rounds or target score
  let matchWinnerId: number | null = null;
  const eligibleWinners = roundBased
    ? players.filter((p) => p.roundsWon >= state.targetScore)
    : players.filter((p) => p.score >= state.targetScore);
  if (eligibleWinners.length > 0) {
    eligibleWinners.sort((a, b) =>
      roundBased ? b.roundsWon - a.roundsWon : b.score - a.score
    );
    matchWinnerId = eligibleWinners[0].id;
  }

  const summary: RoundSummary = {
    roundNumber: state.roundNumber,
    winnerId,
    winnerName: winnerId !== null ? players.find((p) => p.id === winnerId)!.name : 'Draw',
    winType: finalWinType,
    pointsAwarded,
    playerBreakdowns: players.map((p) => ({
      playerId: p.id,
      name: p.name,
      remainingPips: sumPips(p.hand),
      remainingTiles: p.hand,
      totalScore: p.score,
    })),
    matchWinnerId,
  };

  return {
    ...state,
    phase: matchWinnerId !== null ? 'GAME_OVER' : 'ROUND_SUMMARY',
    players,
    roundSummary: summary,
    matchWinnerId,
  };
}

function advanceTurn(state: MatchState, banner: string | null): MatchState {
  const nextIndex = (state.currentPlayerIndex + 1) % state.players.length;
  const nextPlayer = state.players[nextIndex];
  const needsPassScreen =
    state.passAndPlay && !nextPlayer.isAI && state.players.filter((p) => !p.isAI).length > 1;

  return {
    ...state,
    phase: needsPassScreen ? 'PASS_DEVICE' : 'PLAYING',
    currentPlayerIndex: nextIndex,
    actionBanner: banner,
  };
}

export function applyMove(
  state: MatchState,
  tileId: string,
  branch: 'root' | BranchDir
): MatchState {
  const currentPlayer = state.players[state.currentPlayerIndex];
  const tile = currentPlayer.hand.find((t) => t.id === tileId);
  if (!tile) return state;

  const nextSeq = state.moveSequence + 1;
  const nextBoard = placeTileOnBoard(
    state.board,
    tile,
    branch,
    currentPlayer.id,
    nextSeq,
    state.mode
  );

  const nextHand = currentPlayer.hand.filter((t) => t.id !== tileId);
  const openSum = getOpenEndsSum(nextBoard, state.mode);
  const { points, reason } = calculatePlayScore(openSum, state.mode);

  const players = state.players.map((p, idx) =>
    idx === state.currentPlayerIndex
      ? { ...p, hand: nextHand, score: p.score + points }
      : p
  );

  const updatedPlayer = players[state.currentPlayerIndex];
  const scoringEvent =
    points > 0
      ? {
          id: nextSeq,
          playerId: currentPlayer.id,
          playerName: currentPlayer.name,
          points,
          openSum,
          reason,
        }
      : null;

  const baseState: MatchState = {
    ...state,
    players,
    board: nextBoard,
    consecutivePasses: 0,
    moveSequence: nextSeq,
    lastPlayedTileId: tile.id,
    lastDrawnTileId: null,
    lastDrawnPlayerId: null,
    lastScoringEvent: scoringEvent,
  };

  // Immediate target score win check (scoring modes only)
  if (!isRoundBasedMode(state.mode) && updatedPlayer.score >= state.targetScore) {
    const summary: RoundSummary = {
      roundNumber: state.roundNumber,
      winnerId: updatedPlayer.id,
      winnerName: updatedPlayer.name,
      winType: nextHand.length === 0 ? 'domino' : 'domino',
      pointsAwarded: points,
      playerBreakdowns: players.map((p) => ({
        playerId: p.id,
        name: p.name,
        remainingPips: sumPips(p.hand),
        remainingTiles: p.hand,
        totalScore: p.score,
      })),
      matchWinnerId: updatedPlayer.id,
    };
    return {
      ...baseState,
      phase: 'GAME_OVER',
      matchWinnerId: updatedPlayer.id,
      roundSummary: summary,
      actionBanner: `${updatedPlayer.name} reached ${updatedPlayer.score} pts and won!`,
    };
  }

  // Domino out (empty hand)
  if (nextHand.length === 0) {
    return finalizeRound(baseState, 'domino', updatedPlayer.id);
  }

  const banner =
    points > 0
      ? `${currentPlayer.name} played ${tile.a}|${tile.b} (+${points} pts)`
      : `${currentPlayer.name} played ${tile.a}|${tile.b}`;

  return advanceTurn(baseState, banner);
}

export function drawTileFromBoneyard(state: MatchState): MatchState {
  if (!MODE_META[state.mode].hasBoneyard || state.boneyard.length === 0) {
    return state;
  }

  const drawn = state.boneyard[0];
  const remainingBoneyard = state.boneyard.slice(1);
  const currentPlayer = state.players[state.currentPlayerIndex];

  const players = state.players.map((p, idx) =>
    idx === state.currentPlayerIndex ? { ...p, hand: [...p.hand, drawn] } : p
  );

  return {
    ...state,
    players,
    boneyard: remainingBoneyard,
    lastDrawnTileId: drawn.id,
    lastDrawnPlayerId: currentPlayer.id,
    actionBanner: `${currentPlayer.name} drew from boneyard`,
  };
}

export function passCurrentTurn(state: MatchState): MatchState {
  const currentPlayer = state.players[state.currentPlayerIndex];
  const nextPasses = state.consecutivePasses + 1;

  if (nextPasses >= state.players.length) {
    return finalizeRound(
      {
        ...state,
        consecutivePasses: nextPasses,
        lastDrawnTileId: null,
        lastDrawnPlayerId: null,
      },
      'blocked',
      null
    );
  }

  return advanceTurn(
    {
      ...state,
      consecutivePasses: nextPasses,
      lastDrawnTileId: null,
      lastDrawnPlayerId: null,
    },
    `${currentPlayer.name} passed`
  );
}

export function chooseAIMove(state: MatchState): LegalMove | 'draw' | 'pass' {
  const currentPlayer = state.players[state.currentPlayerIndex];
  const legal = getLegalMoves(currentPlayer.hand, state.board, state.mode);

  if (legal.length === 0) {
    if (MODE_META[state.mode].hasBoneyard && state.boneyard.length > 0) {
      return 'draw';
    }
    return 'pass';
  }

  if (state.difficulty === 'easy') {
    return legal[Math.floor(Math.random() * legal.length)];
  }

  // Score every legal move
  const scored = legal.map((move) => {
    const simBoard = placeTileOnBoard(
      state.board,
      move.tile,
      move.branch,
      currentPlayer.id,
      state.moveSequence + 1,
      state.mode
    );
    const openSum = getOpenEndsSum(simBoard, state.mode);
    const { points } = calculatePlayScore(openSum, state.mode);
    const pipWeight = move.tile.a + move.tile.b;
    const isDoubleBonus = move.tile.a === move.tile.b ? 3.5 : 0;

    // Hard AI also values keeping versatile numbers in hand and avoiding exposing easy scores
    let flexibility = 0;
    let dangerPenalty = 0;
    if (state.difficulty === 'hard') {
      const remaining = currentPlayer.hand.filter((t) => t.id !== move.tile.id);
      const nextEnds = getOpenEnds(simBoard, state.mode);
      const matchingInHand = remaining.filter((t) =>
        nextEnds.some((e) => t.a === e.requiredPip || t.b === e.requiredPip)
      ).length;
      flexibility = matchingInHand * 2.5;

      // Immediate win priority
      if (currentPlayer.score + points >= state.targetScore) {
        return { move, utility: 10000 + points };
      }

      // Slight penalty if leaving an open sum close to easy scoring for next player
      if (state.mode === 'all_fives' && openSum % 5 !== 0) {
        dangerPenalty = 0;
      }
    }

    const utility =
      points * 12 +
      pipWeight * 0.8 +
      isDoubleBonus +
      flexibility -
      dangerPenalty;

    return { move, utility };
  });

  scored.sort((a, b) => b.utility - a.utility);

  if (state.difficulty === 'medium' && scored.length > 1 && Math.random() < 0.25) {
    return scored[1].move;
  }

  return scored[0].move;
}
