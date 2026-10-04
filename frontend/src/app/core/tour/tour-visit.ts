import { Service } from '@angular/core';

import { STORAGE_KEY } from './tour-visit.constants';
import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

/**
 * Sole owner of the flag recording that the visitor went through the one-time tour.
 */
@Service()
export class TourVisit {
  /**
   * Whether the tour completion is recorded.
   */
  public hasCompleted(): boolean {
    return readStorage(STORAGE_KEY) !== null;
  }

  /**
   * Records the completion, so later visits skip the tour.
   */
  public markCompleted(): void {
    writeStorage(STORAGE_KEY, 'true');
  }
}
