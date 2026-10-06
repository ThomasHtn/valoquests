/**
 * Build-time configuration, swapped per build configuration by `fileReplacements` in `angular.json`.
 */
export interface Environment {
  /**
   * Base URL of every backend endpoint, without a trailing slash; relative so `/api` stays same-origin.
   */
  readonly apiBaseUrl: string;
}
