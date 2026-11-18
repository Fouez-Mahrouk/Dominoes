import { GameSettings, LifetimeStats, MatchState } from '../types/domino';

const SETTINGS_KEY = 'dominoes_studio_settings_v1';
const MATCH_KEY = 'dominoes_studio_match_v1';
const STATS_KEY = 'dominoes_studio_stats_v1';

export const DEFAULT_SETTINGS: GameSettings = {
  theme: 'system',
  vibrationEnabled: true,
  soundEnabled: true,
  difficulty: 'medium',
  showModeTipOnStart: true,
};

export const DEFAULT_STATS: LifetimeStats = {
  matchesPlayed: 0,
  matchesWon: 0,
  roundsWon: 0,
  highestScore: 0,
  totalPointsScored: 0,
};

export function loadSettings(): GameSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveSettings(settings: GameSettings): void {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch {
    // Ignore storage quota errors
  }
}

export function loadSavedMatch(): MatchState | null {
  try {
    const raw = localStorage.getItem(MATCH_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as MatchState;
    if (!parsed || !parsed.players || parsed.players.length < 2) return null;
    if (parsed.phase === 'MENU' || parsed.phase === 'GAME_OVER') return null;
    const isRoundMode = parsed.mode === 'free_game' || parsed.mode === 'block';
    if (isRoundMode && ![1, 3, 5, 7].includes(parsed.targetScore)) {
      parsed.targetScore = 3;
    } else if (!isRoundMode && ![50, 100, 150, 200].includes(parsed.targetScore)) {
      parsed.targetScore = 100;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function saveMatch(state: MatchState | null): void {
  try {
    if (!state || state.phase === 'MENU' || state.phase === 'GAME_OVER') {
      localStorage.removeItem(MATCH_KEY);
      return;
    }
    localStorage.setItem(MATCH_KEY, JSON.stringify(state));
  } catch {
    // Ignore storage quota errors
  }
}

export function loadStats(): LifetimeStats {
  try {
    const raw = localStorage.getItem(STATS_KEY);
    if (!raw) return DEFAULT_STATS;
    return { ...DEFAULT_STATS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_STATS;
  }
}

export function saveStats(stats: LifetimeStats): void {
  try {
    localStorage.setItem(STATS_KEY, JSON.stringify(stats));
  } catch {
    // Ignore storage quota errors
  }
}

export function resetAllStats(): LifetimeStats {
  try {
    localStorage.removeItem(STATS_KEY);
    localStorage.removeItem(MATCH_KEY);
  } catch {
    // Ignore
  }
  return DEFAULT_STATS;
}
