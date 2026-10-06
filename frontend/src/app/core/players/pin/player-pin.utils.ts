import { readStorage, removeStorage, writeStorage } from '@core/storage/safe-storage.utils';

import { PINNED_PLAYER_KEY } from './player-pin.constants';

/**
 * Pinned player, `null` when absent, malformed or storage fails.
 */
export function readPinnedPlayer(): number | null {
  const stored = Number(readStorage(PINNED_PLAYER_KEY));
  return Number.isInteger(stored) && stored > 0 ? stored : null;
}

/**
 * Stores the pinned player, clears it on `null`; storage failures are ignored.
 */
export function writePinnedPlayer(playerId: number | null): void {
  if (playerId === null) {
    removeStorage(PINNED_PLAYER_KEY);
  } else {
    writeStorage(PINNED_PLAYER_KEY, String(playerId));
  }
}
