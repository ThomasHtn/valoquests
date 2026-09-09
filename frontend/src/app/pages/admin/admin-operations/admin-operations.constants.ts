/**
 * Delay between two polls of the running synchronization, in milliseconds.
 *
 * A synchronization walks the Henrik match history under a rate limit of a few dozen requests per
 * minute, so its counters move in steps of seconds at best. Polling faster would only multiply
 * requests against a status that has not changed.
 */
export const SYNCHRONIZATION_POLL_INTERVAL_MS = 3_000;
