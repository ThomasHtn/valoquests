import { Service, signal } from '@angular/core';

import { ADMIN_KEY_STORAGE_KEY } from './admin-session.constants';

/**
 * Holds the administrator key for the duration of a backoffice session.
 *
 * The backoffice has no user accounts: the single `ADMIN_API_KEY` the backend checks is the whole
 * credential, so "being signed in" means nothing more than holding a key the API accepts. This
 * service is the only place that reads or writes it, so the storage key never leaks into
 * components, and the signal lets the sidebar swap its entries the moment a session opens or
 * closes.
 *
 * The key is readable by any script running on the page. That is acceptable here — a single key,
 * shared with the coach, on a personal project — and it is why the key lives in `sessionStorage`
 * rather than `localStorage`: it dies with the tab instead of waiting there for the next visitor.
 */
@Service()
export class AdminSession {
  /**
   * Administrator key of the open session, or `null` when there is none.
   */
  private readonly currentKey = signal<string | null>(
    sessionStorage.getItem(ADMIN_KEY_STORAGE_KEY),
  );

  /**
   * Administrator key of the open session, as a read-only signal.
   */
  public readonly key = this.currentKey.asReadonly();

  /**
   * Whether a session is currently open.
   *
   * Only says a key is held, never that the backend still accepts it. A key revoked server-side is
   * discovered on the next request, which is what the HTTP interceptor turns back into a signed-out
   * session.
   *
   * @returns Whether an administrator key is held.
   */
  public isAuthenticated(): boolean {
    return this.currentKey() !== null;
  }

  /**
   * Opens a session with a key the backend has already accepted.
   *
   * @param key - The administrator key.
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
