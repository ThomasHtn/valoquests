import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { REPLAY_QUERY_PARAM } from '@core/landing/landing-visit.constants';

import { TourVisit } from './tour-visit';

/**
 * Keeps the guided tour as a one-time briefing.
 *
 * Lets the tour render for a visitor who has never been through it, and redirects everyone else
 * straight to the overview. The replay escape hatch is the same URL convention as the landing
 * page's — reused rather than redeclared — and is what the "replay the tour" link on the rules page
 * relies on.
 *
 * @param route - Snapshot of the route being activated, read for the replay query parameter.
 * @returns `true` to render the tour, or a redirect to the overview.
 */
export const tourEntryGuard: CanActivateFn = (route) => {
  const isReplay = route.queryParamMap.has(REPLAY_QUERY_PARAM);
  if (isReplay || !inject(TourVisit).hasCompleted()) {
    return true;
  }

  return inject(Router).createUrlTree(['/overview']);
};
