import { Service } from '@angular/core';

import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

import { LANDING_ENTERED_STORAGE_KEY } from './landing-visit.constants';

/**
 * Sole owner of the flag recording that the visitor crossed the one-time landing page.
 */
@Service()
export class LandingVisit {
  /**
   * Whether the entry through the landing page is recorded.
   */
  public hasEntered(): boolean {
    return readStorage(LANDING_ENTERED_STORAGE_KEY) !== null;
  }

  /**
   * Records the entry, so later visits skip the landing.
   */
  public markEntered(): void {
    writeStorage(LANDING_ENTERED_STORAGE_KEY, 'true');
  }
}
