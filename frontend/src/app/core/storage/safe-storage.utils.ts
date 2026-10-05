import { StorageArea } from './safe-storage.model';

/**
 * Storage of an area; reading the global itself throws when site data is blocked.
 */
function storageOf(area: StorageArea): Storage {
  return area === 'session' ? sessionStorage : localStorage;
}

/**
 * Stored value, `null` when missing or when storage throws (private window, blocked).
 */
export function readStorage(key: string, area: StorageArea = 'local'): string | null {
  try {
    return storageOf(area).getItem(key);
  } catch {
    return null;
  }
}

/**
 * Writes a value, silently skipped when storage is unavailable since it is only a convenience.
 */
export function writeStorage(key: string, value: string, area: StorageArea = 'local'): void {
  try {
    storageOf(area).setItem(key, value);
  } catch {
    // Unavailable storage: the choice is not remembered.
  }
}

/**
 * Removes a value, silently skipped when storage is unavailable.
 */
export function removeStorage(key: string, area: StorageArea = 'local'): void {
  try {
    storageOf(area).removeItem(key);
  } catch {
    // Unavailable storage: nothing was remembered either.
  }
}
