import { HttpErrorResponse } from '@angular/common/http';

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
