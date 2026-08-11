import { Service } from '@angular/core';

import { STORAGE_KEY } from './landing-visit.constants';

/**
 * Tracks whether the visitor has already entered the application through the landing page.
 *
 * The landing page is a one-time doorway: once crossed, the root route redirects straight to the
 * overview instead of asking the visitor to click the compass again on every visit. This service is
 * the only place that reads or writes the flag, so the storage key never leaks into components.
 */
@Service()
export class LandingVisit {
  /**
   * Whether the visitor has already entered the application through the landing page.
   *
   * @returns Whether the entry has been recorded.
   */
  public hasEntered(): boolean {
    return localStorage.getItem(STORAGE_KEY) !== null;
  }

  /**
   * Records that the visitor has entered the application, so subsequent visits skip the landing.
   */
  public markEntered(): void {
    localStorage.setItem(STORAGE_KEY, 'true');
  }
}
