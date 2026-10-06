/**
 * Public synchronization status from `GET /api/synchronization/status`.
 */
export interface SynchronizationStatus {
  /**
   * Whether a synchronization is pending or running, rebuilds included.
   */
  readonly inProgress: boolean;

  /**
   * ISO instant the last successful synchronization finished its rebuilds, `null` if never.
   */
  readonly lastCompletedAt: string | null;

  /**
   * ISO instant the last synchronization that imported matches finished, `null` if never.
   */
  readonly lastImportedAt: string | null;
}
