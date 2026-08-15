import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { ADMIN_LOGIN_ROUTE } from './admin-session.constants';
import { AdminSession } from './admin-session';

/**
 * Keeps the backoffice behind an open session.
 *
 * Only checks that a key is held: whether the backend still accepts it is settled by the first
 * request the page makes, and answering that here would mean an HTTP round trip before every
 * navigation. A key the backend rejects lands the visitor back on the login screen through
 * `adminKeyInterceptor`, so the two cases converge.
 *
 * This is not a security boundary — the API is. It only spares the visitor a screen full of
 * failed requests.
 *
 * @returns `true` to render the requested backoffice page, or a redirect to the login screen.
 */
export const adminGuard: CanActivateFn = () => {
  if (inject(AdminSession).isAuthenticated()) {
    return true;
  }

  return inject(Router).createUrlTree([ADMIN_LOGIN_ROUTE]);
};
