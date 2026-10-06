import { Service } from '@angular/core';

import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

import { TOUR_COMPLETED_STORAGE_KEY } from './tour-visit.constants';

/**
 * Sole owner of the flag recording that the visitor went through the one-time tour.
 */
@Service()
export class TourVisit {
  /**
   * Whether the tour completion is recorded.
   */
  public hasCompleted(): boolean {
    return readStorage(TOUR_COMPLETED_STORAGE_KEY) !== null;
  }

  /**
   * Records the completion, so later visits skip the tour.
   */
  public markCompleted(): void {
    writeStorage(TOUR_COMPLETED_STORAGE_KEY, 'true');
  }
}
