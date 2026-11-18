export type GameMode =
  | 'free_game'
  | 'block'
  | 'all_fives'
  | 'all_threes'
  | 'fives_and_threes';

export type Difficulty = 'easy' | 'medium' | 'hard';

export type ThemeMode = 'system' | 'light' | 'dark';

export type PlayerCount = 2 | 3 | 4;

export type TargetScore = 1 | 3 | 5 | 7 | 50 | 100 | 150 | 200;

export type BranchDir = 'left' | 'right' | 'up' | 'down';

export interface DominoTile {
  id: string;
  a: number;
  b: number;
}

export interface PlacedTile {
  tile: DominoTile;
  /** Which branch this tile belongs to: 'root' for first tile, else 'left' | 'right' | 'up' | 'down' */
  branch: 'root' | BranchDir;
  /** Pip facing toward the parent/root connection (or left/top if root) */
  innerPip: number;
  /** Pip facing outward toward the open tip (or right/bottom if root) */
  outerPip: number;
  isDouble: boolean;
  isSpinner: boolean;
  playedBy: number;
  sequence: number;
}

export interface OpenEnd {
  branch: BranchDir;
  requiredPip: number;
  /** Exposed pip value contributed to open-end scoring total */
  scoreValue: number;
  /** True if this open end belongs to a double tile showing both faces */
  isDoubleTip: boolean;
  /** Anchor tile id this end attaches to */
  anchorTileId: string;
}

export interface Player {
  id: number;
  name: string;
  isAI: boolean;
  avatarIndex: number;
  hand: DominoTile[];
  score: number;
  roundsWon: number;
}

export interface GameSettings {
  theme: ThemeMode;
  vibrationEnabled: boolean;
  soundEnabled: boolean;
  difficulty: Difficulty;
  showModeTipOnStart: boolean;
}

export interface ScoringEvent {
  id: number;
  playerId: number;
  playerName: string;
  points: number;
  openSum: number;
  reason: string;
}

export interface RoundSummary {
  roundNumber: number;
  winnerId: number | null;
  winnerName: string;
  winType: 'domino' | 'blocked' | 'tie';
  pointsAwarded: number;
  playerBreakdowns: {
    playerId: number;
    name: string;
    remainingPips: number;
    remainingTiles: DominoTile[];
    totalScore: number;
  }[];
  matchWinnerId: number | null;
}

export type GamePhase =
  | 'MENU'
  | 'PLAYING'
  | 'PASS_DEVICE'
  | 'ROUND_SUMMARY'
  | 'GAME_OVER';

export interface BoardBranches {
  root: PlacedTile | null;
  left: PlacedTile[];
  right: PlacedTile[];
  up: PlacedTile[];
  down: PlacedTile[];
  spinnerTileId: string | null;
  spinnerLocation: 'root' | 'left' | 'right' | null;
}

export interface MatchState {
  phase: GamePhase;
  mode: GameMode;
  playerCount: PlayerCount;
  passAndPlay: boolean;
  targetScore: TargetScore;
  difficulty: Difficulty;
  roundNumber: number;
  players: Player[];
  currentPlayerIndex: number;
  boneyard: DominoTile[];
  board: BoardBranches;
  consecutivePasses: number;
  moveSequence: number;
  lastPlayedTileId: string | null;
  lastDrawnTileId: string | null;
  lastDrawnPlayerId: number | null;
  lastScoringEvent: ScoringEvent | null;
  roundSummary: RoundSummary | null;
  matchWinnerId: number | null;
  actionBanner: string | null;
}

export interface LifetimeStats {
  matchesPlayed: number;
  matchesWon: number;
  roundsWon: number;
  highestScore: number;
  totalPointsScored: number;
}
