export type HapticType = 'pickup' | 'place' | 'score' | 'win' | 'error';

const HAPTIC_PATTERNS: Record<HapticType, number | number[]> = {
  pickup: 12,
  place: 28,
  score: [40, 45, 70],
  win: [75, 45, 75, 45, 160],
  error: [18, 30, 18],
};

export function triggerHaptic(type: HapticType, enabled: boolean): void {
  if (!enabled) return;
  if (typeof navigator === 'undefined' || typeof navigator.vibrate !== 'function') {
    return;
  }
  try {
    navigator.vibrate(HAPTIC_PATTERNS[type]);
  } catch {
    // Ignore environments where vibration is blocked
  }
}
