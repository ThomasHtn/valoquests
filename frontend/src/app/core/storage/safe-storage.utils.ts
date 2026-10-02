/**
 * Reads a value from `localStorage`, `null` when storage is unavailable (private window, blocked
 * site data), which throws on access in some browsers.
 *
 * @param key - The storage key.
 * @returns The stored value, or `null`.
 */
export function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

/**
 * Writes a value to `localStorage`, silently doing nothing when storage is unavailable: what it
 * remembers is a convenience, never something the application needs to run.
 *
 * @param key - The storage key.
 * @param value - The value to store.
 */
export function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Unavailable storage: the choice simply is not remembered.
  }
}
