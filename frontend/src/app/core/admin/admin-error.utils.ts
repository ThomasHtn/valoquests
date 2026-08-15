import { HttpErrorResponse } from '@angular/common/http';

/**
 * Shape of the backend's RFC 7807 problem response.
 */
interface ApiProblem {
  readonly detail?: string;
  readonly errors?: Record<string, string>;
}

/**
 * Extracts a message worth showing from a failed administration request.
 *
 * The backend writes `detail` for the caller — "a synchronization is already in progress", "the
 * Riot identity is already tracked" — and it is always more useful than anything the frontend could
 * infer from a status code. Validation failures carry their explanation per field instead, so those
 * are joined rather than dropped.
 *
 * @param error - The rejected request's error.
 * @param fallback - Already-translated message used when the response carries nothing readable.
 * @returns The message to show.
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
