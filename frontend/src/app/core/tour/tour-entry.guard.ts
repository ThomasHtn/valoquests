import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { TourVisit } from './tour-visit';
import { REPLAY_STATE_KEY } from './tour-visit.constants';

/**
 * Keeps the guided tour as a one-time briefing.
 *
 * Lets the tour render for a visitor who has never been through it, and redirects everyone else
 * straight to the overview. The replay escape hatch is the {@link REPLAY_STATE_KEY} navigation
 * state set by the "replay the tour" link on the rules page, never a URL parameter.
 *
 * @returns `true` to render the tour, or a redirect to the overview.
 */
export const tourEntryGuard: CanActivateFn = () => {
  const router = inject(Router);
  const isReplay = router.currentNavigation()?.extras.state?.[REPLAY_STATE_KEY] === true;
  if (isReplay || !inject(TourVisit).hasCompleted()) {
    return true;
  }

  return router.createUrlTree(['/overview']);
};
