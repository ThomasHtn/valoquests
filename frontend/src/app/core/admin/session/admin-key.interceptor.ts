import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { ADMIN_API_PREFIX, ADMIN_KEY_HEADER, ADMIN_LOGIN_ROUTE } from './admin-session.constants';
import { AdminSession } from './admin-session';

/**
 * Adds the admin key to `/api/admin` requests only, and signs out on a 401 or 403.
 * Requests already carrying the header (the sign-in probe) are left alone, failures included.
 */
export const adminKeyInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(ADMIN_API_PREFIX) || request.headers.has(ADMIN_KEY_HEADER)) {
    return next(request);
  }

  const session = inject(AdminSession);
  const router = inject(Router);
  const key = session.key();

  const authenticated =
    key === null ? request : request.clone({ setHeaders: { [ADMIN_KEY_HEADER]: key } });

  return next(authenticated).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
        session.signOut();
        void router.navigate([ADMIN_LOGIN_ROUTE]);
      }

      return throwError(() => error);
    }),
  );
};
