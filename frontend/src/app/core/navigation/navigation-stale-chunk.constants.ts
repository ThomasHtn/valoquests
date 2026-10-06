/**
 * Session key holding the last URL reloaded for a stale chunk, so a lasting failure does not loop.
 */
export const STALE_CHUNK_RELOAD_KEY = 'valoquests.staleChunkReload';

/**
 * Messages browsers give a lazy chunk that failed to load (Chromium, Firefox, Safari).
 */
export const STALE_CHUNK_MESSAGE_PATTERN =
  /dynamically imported module|Importing a module script failed/i;
