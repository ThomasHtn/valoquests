import { Service, signal } from '@angular/core';

import { ADMIN_KEY_STORAGE_KEY } from './admin-session.constants';

/**
 * Holds the admin key, the whole credential, in `sessionStorage` so it dies with the tab.
 */
@Service()
export class AdminSession {
  /**
   * Admin key of the open session, `null` when none.
   */
  private readonly currentKey = signal<string | null>(
    sessionStorage.getItem(ADMIN_KEY_STORAGE_KEY),
  );

  /**
   * Read-only admin key of the open session.
   */
  public readonly key = this.currentKey.asReadonly();

  /**
   * Whether a key is held, not whether the backend still accepts it.
   */
  public isAuthenticated(): boolean {
    return this.currentKey() !== null;
  }

  /**
   * Opens a session with a key the backend already accepted.
   */
  public signIn(key: string): void {
    sessionStorage.setItem(ADMIN_KEY_STORAGE_KEY, key);
    this.currentKey.set(key);
  }

  /**
   * Closes the session and forgets the key.
   */
  public signOut(): void {
    sessionStorage.removeItem(ADMIN_KEY_STORAGE_KEY);
    this.currentKey.set(null);
  }
}
