import { PINNED_PLAYER_KEY } from './player-pin.constants';

/**
 * Pinned player, `null` when absent, malformed or storage fails.
 */
export function readPinnedPlayer(): number | null {
  try {
    const stored = Number(localStorage.getItem(PINNED_PLAYER_KEY));
    return Number.isInteger(stored) && stored > 0 ? stored : null;
  } catch {
    return null;
  }
}

/**
 * Stores the pinned player, clears it on `null`; storage failures are ignored.
 */
export function writePinnedPlayer(playerId: number | null): void {
  try {
    if (playerId === null) {
      localStorage.removeItem(PINNED_PLAYER_KEY);
    } else {
      localStorage.setItem(PINNED_PLAYER_KEY, String(playerId));
    }
  } catch {
    // Ignored: the board falls back to its own order.
  }
}
