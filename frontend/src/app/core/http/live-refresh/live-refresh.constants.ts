/**
 * Polling period of the synchronization status.
 */
export const LIVE_REFRESH_POLL_MS = 60_000;

/**
 * Delay after campaign midnight before the day counts as turned, leaving the 00:10 tick room.
 */
export const DAY_TURN_GRACE_MS = 15 * 60_000;
