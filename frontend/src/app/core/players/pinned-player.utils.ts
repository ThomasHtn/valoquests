import { PINNED_PLAYER_KEY } from './pinned-player.constants';

/**
 * The player the reader pinned, or `null`; a malformed value or a storage failure reads as none.
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
 * Remembers the pinned operator, or forgets it on `null`; storage failures are ignored.
 */
export function writePinnedPlayer(playerId: number | null): void {
  try {
    if (playerId === null) {
      localStorage.removeItem(PINNED_PLAYER_KEY);
    } else {
      localStorage.setItem(PINNED_PLAYER_KEY, String(playerId));
    }
  } catch {
    // Nothing to do: the board opens in its own order next time.
  }
}
