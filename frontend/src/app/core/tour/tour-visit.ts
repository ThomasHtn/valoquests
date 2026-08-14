import { Service } from '@angular/core';

import { STORAGE_KEY } from './tour-visit.constants';

/**
 * Tracks whether the visitor has already been through the guided tour.
 *
 * The tour is a one-time briefing sitting between the landing page and the overview: once walked
 * through or skipped, it steps aside and the visitor goes straight to the application. This service
 * is the only place that reads or writes the flag, so the storage key never leaks into components.
 */
@Service()
export class TourVisit {
  /**
   * Whether the visitor has already been through the guided tour.
   *
   * @returns Whether the completion has been recorded.
   */
  public hasCompleted(): boolean {
    return localStorage.getItem(STORAGE_KEY) !== null;
  }

  /**
   * Records that the visitor has been through the guided tour, so subsequent visits skip it.
   */
  public markCompleted(): void {
    localStorage.setItem(STORAGE_KEY, 'true');
  }
}
