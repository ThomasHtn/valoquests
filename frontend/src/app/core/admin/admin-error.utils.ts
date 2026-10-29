import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';

import { ApiProblem } from './admin-error.model';

/**
 * Failed admin request message: field errors, else the backend `detail`, else `fallback`.
 */
export function resolveAdminErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const problem = error.error as ApiProblem | null;

  if (problem?.errors) {
    const fieldErrors = Object.entries(problem.errors)
      .map(([field, message]) => `${field}: ${message}`)
      .join(' — ');

    if (fieldErrors !== '') {
      return fieldErrors;
    }
  }

  return problem?.detail ?? fallback;
}

/**
 * Sign-in failure as a translation key: the backend words its refusals in English only.
 */
export function resolveSignInErrorKey(error: unknown): string | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  switch (error.status) {
    case HttpStatusCode.Unauthorized:
    case HttpStatusCode.Forbidden:
      return 'admin.login.rejected';
    case HttpStatusCode.TooManyRequests:
      return 'admin.login.rateLimited';
    default:
      return null;
  }
}
