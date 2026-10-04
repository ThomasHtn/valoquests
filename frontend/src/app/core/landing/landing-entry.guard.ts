import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { REPLAY_QUERY_PARAM } from './landing-visit.constants';
import { LandingVisit } from './landing-visit';

/**
 * Shows the landing on a first visit or with {@link REPLAY_QUERY_PARAM}, else the overview.
 */
export const landingEntryGuard: CanActivateFn = (route) => {
  const isReplay = route.queryParamMap.has(REPLAY_QUERY_PARAM);
  if (isReplay || !inject(LandingVisit).hasEntered()) {
    return true;
  }

  return inject(Router).createUrlTree(['/overview']);
};
