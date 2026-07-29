/**
 * Build-time configuration of the application.
 *
 * Values are resolved at build time through the `fileReplacements` entry declared in
 * `angular.json`, which swaps `environment.ts` for `environment.development.ts` in the
 * development configuration.
 */
export interface Environment {
  /**
   * Base URL every backend endpoint is appended to, without a trailing slash.
   *
   * Kept relative by default because the backend serves the built frontend from the same origin
   * in production, and the dev server proxies `/api` to `localhost:8080` (see `proxy.conf.json`).
   * Point it at an absolute URL when the API is deployed on a different origin.
   */
  readonly apiBaseUrl: string;
}
