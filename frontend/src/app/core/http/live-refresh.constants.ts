/**
 * How often the roster is re-read to detect a change on the backend.
 */
export const LIVE_REFRESH_POLL_MS = 60_000;

/**
 * Quiet time after the last detected change before the screens reload.
 *
 * A batch synchronizes players one after the other, each moving the roster's stamp, so it can span
 * several polls. Longer than {@link LIVE_REFRESH_POLL_MS} so the whole batch reloads the screens
 * once, and the replay following the last player has time to commit.
 */
export const LIVE_REFRESH_SETTLE_MS = 90_000;
