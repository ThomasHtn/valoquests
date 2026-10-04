import { Service } from '@angular/core';

import { STORAGE_KEY } from './landing-visit.constants';
import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

/**
 * Sole owner of the flag recording that the visitor crossed the one-time landing page.
 */
@Service()
export class LandingVisit {
  /**
   * Whether the entry through the landing page is recorded.
   */
  public hasEntered(): boolean {
    return readStorage(STORAGE_KEY) !== null;
  }

  /**
   * Records the entry, so later visits skip the landing.
   */
  public markEntered(): void {
    writeStorage(STORAGE_KEY, 'true');
  }
}
