/**
 * How often the roster is re-read to detect a change on the backend.
 */
export const LIVE_REFRESH_POLL_MS = 60_000;

/**
 * Delay between detecting a change and reloading the screens.
 *
 * A player's synchronization instant is written before the replay that follows it has committed, so
 * a reload fired the second the change is seen could still read the base as it stood before.
 */
export const LIVE_REFRESH_SETTLE_MS = 20_000;
