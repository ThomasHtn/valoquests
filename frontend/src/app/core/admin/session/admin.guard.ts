import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AdminSession } from './admin-session';
import { ADMIN_LOGIN_ROUTE } from './admin-session.constants';

/**
 * Keeps the backoffice behind an open session; only checks a key is held, the API is the guard.
 * A rejected key is handled by `adminKeyInterceptor`, avoiding a round trip per navigation.
 */
export const adminGuard: CanActivateFn = () => {
  if (inject(AdminSession).isAuthenticated()) {
    return true;
  }

  return inject(Router).createUrlTree([ADMIN_LOGIN_ROUTE]);
};
