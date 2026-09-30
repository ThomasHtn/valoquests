/**
 * Public synchronization status, as returned by `GET /api/synchronization/status`.
 */
export interface SynchronizationStatus {
  /**
   * Whether a synchronization is pending or running, challenge and campaign rebuild included.
   */
  readonly inProgress: boolean;

  /**
   * Instant the last successful synchronization finished, once challenges and campaign were
   * rebuilt, as an ISO-8601 instant, or `null` when none ever did.
   */
  readonly lastCompletedAt: string | null;
}
