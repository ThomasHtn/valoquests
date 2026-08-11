import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { REPLAY_QUERY_PARAM } from './landing-visit.constants';
import { LandingVisit } from './landing-visit';

/**
 * Keeps the landing page as a one-time doorway.
 *
 * Lets the landing page render on a first visit, and redirects returning visitors straight to the
 * overview. The {@link REPLAY_QUERY_PARAM} escape hatch re-opens it on demand, since there is
 * otherwise no way back to it once the entry has been recorded.
 *
 * @param route - Snapshot of the route being activated, read for the replay query parameter.
 * @returns `true` to render the landing page, or a redirect to the overview.
 */
export const landingEntryGuard: CanActivateFn = (route) => {
  const isReplay = route.queryParamMap.has(REPLAY_QUERY_PARAM);
  if (isReplay || !inject(LandingVisit).hasEntered()) {
    return true;
  }

  return inject(Router).createUrlTree(['/overview']);
};
