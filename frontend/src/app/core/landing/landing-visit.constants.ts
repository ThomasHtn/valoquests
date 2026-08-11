/**
 * `localStorage` key under which the first entry through the landing page is recorded.
 */
export const STORAGE_KEY = 'valorant-tracker.landing-entered';

/**
 * Query parameter that re-opens the landing page after it has already been entered once, without
 * having to clear {@link STORAGE_KEY} by hand.
 */
export const REPLAY_QUERY_PARAM = 'replay';
