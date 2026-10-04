/**
 * `localStorage` value, `null` when missing or when storage throws (private window, blocked).
 */
export function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

/**
 * Writes to `localStorage`, silently skipped when unavailable since it is only a convenience.
 */
export function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Unavailable storage: the choice is not remembered.
  }
}
