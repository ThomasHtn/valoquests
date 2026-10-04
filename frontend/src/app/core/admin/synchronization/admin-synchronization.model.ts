/**
 * Mirrors the backend `SynchronizationStatus`; all but `PENDING` and `RUNNING` are terminal.
 */
export type SynchronizationStatus =
  'PENDING' | 'RUNNING' | 'PARTIAL' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

/**
 * One synchronization run; mirrors the backend `SynchronizationResponse`.
 */
export interface SynchronizationExecution {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Kind of run, as named by the backend.
   */
  readonly type: string;

  /**
   * Whether the run was started by hand or by the scheduler.
   */
  readonly trigger: 'SCHEDULED' | 'MANUAL';

  /**
   * Outcome of the run.
   */
  readonly status: SynchronizationStatus;

  /**
   * ISO-8601 instant the run started at, or `null` when it never did.
   */
  readonly startedAt: string | null;

  /**
   * ISO-8601 instant the run finished at, or `null` while it is still in flight.
   */
  readonly finishedAt: string | null;

  /**
   * Instant of the last run, ISO-8601, or `null` when none ran.
   */
  readonly lastAttemptAt: string | null;

  /**
   * Instant of the last successful run, ISO-8601, or `null` when none succeeded.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;

  /**
   * Players the run covered.
   */
  readonly playersProcessed: number;

  /**
   * Players whose synchronization failed.
   */
  readonly failureCount: number;

  /**
   * Matches imported by the run.
   */
  readonly matchesImported: number;

  /**
   * Aggregated failure description, or `null` when the run reported none.
   */
  readonly errorMessage: string | null;
}

/**
 * One synchronization with per-player outcomes; mirrors `SynchronizationDetailsResponse`.
 */
export interface SynchronizationDetails {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Kind of run, as named by the backend.
   */
  readonly type: string;

  /**
   * What started the run: the scheduler or an operator.
   */
  readonly trigger: 'SCHEDULED' | 'MANUAL';

  /**
   * Outcome of the run.
   */
  readonly status: SynchronizationStatus;

  /**
   * Start instant, ISO-8601, or `null` before the run started.
   */
  readonly startedAt: string | null;

  /**
   * End instant, ISO-8601, or `null` while running.
   */
  readonly finishedAt: string | null;

  /**
   * Players the run covered.
   */
  readonly playersProcessed: number;

  /**
   * Players whose synchronization failed.
   */
  readonly failureCount: number;

  /**
   * Matches imported by the run.
   */
  readonly matchesImported: number;

  /**
   * Stored failure message, or `null` when none.
   */
  readonly errorMessage: string | null;

  /**
   * One result per player the run covered.
   */
  readonly players: readonly SynchronizationPlayerResult[];
}

/**
 * One player's outcome in a synchronization; mirrors `PlayerResultResponse`.
 */
export interface SynchronizationPlayerResult {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Outcome for this player.
   */
  readonly status: SynchronizationStatus;

  /**
   * Henrik pages fetched for the player.
   */
  readonly pagesFetched: number;

  /**
   * Matches imported by the run.
   */
  readonly matchesImported: number;

  /**
   * Stored failure message, or `null` when none.
   */
  readonly errorMessage: string | null;

  /**
   * What ended the match-history walk, `null` when it failed before completing.
   */
  readonly stopReason: string | null;
}
