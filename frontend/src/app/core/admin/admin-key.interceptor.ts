import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '@env/environment';

import { ADMIN_KEY_HEADER, ADMIN_LOGIN_ROUTE } from './admin-session.constants';
import { AdminSession } from './admin-session';

/**
 * URL prefix identifying the administration API.
 */
const ADMIN_API_PREFIX = `${environment.apiBaseUrl}/admin`;

/**
 * Attaches the administrator key to administration requests, and ends the session when the backend
 * stops accepting it.
 *
 * Scoped to `/api/admin` on purpose: the public API needs no credential, and sending one on every
 * request would hand the key to routes that have no business seeing it.
 *
 * A request that already carries the header is left alone, failures included. That is the sign-in
 * probe testing a key the session does not hold yet: signing out over its rejection would be
 * meaningless, and the login screen needs the error to tell a missing key from a wrong one.
 *
 * For every other administration request, a 401 or 403 is the only way the frontend learns the key
 * stopped working — it cannot tell a revoked key from a redeployed backend, and does not need to.
 * Both mean the same thing: what is held is unusable, so it is dropped and the login screen takes
 * over, rather than letting every subsequent screen fail on its own.
 *
 * @param request - The outgoing request.
 * @param next - The next handler in the chain.
 * @returns The handled request stream.
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
