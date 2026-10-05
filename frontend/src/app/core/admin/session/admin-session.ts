import { Service, signal } from '@angular/core';

import { readStorage, removeStorage, writeStorage } from '@core/storage/safe-storage.utils';
import { ADMIN_KEY_STORAGE_KEY } from './admin-session.constants';

/**
 * Holds the admin key, the whole credential, in `sessionStorage` so it dies with the tab.
 * Blocked storage only means the key is asked again after a reload.
 */
@Service()
export class AdminSession {
  /**
   * Admin key of the open session, `null` when none.
   */
  private readonly currentKey = signal<string | null>(
    readStorage(ADMIN_KEY_STORAGE_KEY, 'session'),
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
    writeStorage(ADMIN_KEY_STORAGE_KEY, key, 'session');
    this.currentKey.set(key);
  }

  /**
   * Closes the session and forgets the key.
   */
  public signOut(): void {
    removeStorage(ADMIN_KEY_STORAGE_KEY, 'session');
    this.currentKey.set(null);
  }
}
