import { NavigationError } from '@angular/router';

import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

import {
  STALE_CHUNK_MESSAGE_PATTERN,
  STALE_CHUNK_RELOAD_KEY,
} from './navigation-stale-chunk.constants';

/**
 * Whether a navigation failed on a lazy chunk the server no longer serves, as after a redeploy.
 */
export function isStaleChunkError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return STALE_CHUNK_MESSAGE_PATTERN.test(message);
}

/**
 * Loads the target page in full, once per URL and session, so a tab older than the deploy picks up the new bundle.
 */
export function reloadOnStaleChunk(event: NavigationError): void {
  if (
    !isStaleChunkError(event.error) ||
    readStorage(STALE_CHUNK_RELOAD_KEY, 'session') === event.url
  ) {
    return;
  }
  writeStorage(STALE_CHUNK_RELOAD_KEY, event.url, 'session');
  location.assign(event.url);
}
