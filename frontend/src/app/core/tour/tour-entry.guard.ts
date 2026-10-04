import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { TourVisit } from './tour-visit';
import { REPLAY_STATE_KEY } from './tour-visit.constants';

/**
 * Shows the tour once, or on replay via the {@link REPLAY_STATE_KEY} state; else the overview.
 */
export const tourEntryGuard: CanActivateFn = () => {
  const router = inject(Router);
  const isReplay = router.currentNavigation()?.extras.state?.[REPLAY_STATE_KEY] === true;
  if (isReplay || !inject(TourVisit).hasCompleted()) {
    return true;
  }

  return router.createUrlTree(['/overview']);
};
