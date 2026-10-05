/**
 * Mirrors the backend `SynchronizationStatus`; all but `PENDING` and `RUNNING` are terminal.
 */
export type SynchronizationRunStatus =
  'PENDING' | 'RUNNING' | 'PARTIAL' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

/**
 * What started a run: the scheduler or an operator.
 */
type SynchronizationTrigger = 'SCHEDULED' | 'MANUAL';

/**
 * Fields every view of a synchronization run shares.
 */
interface SynchronizationRun {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Kind of run, as named by the backend.
   */
  readonly type: string;

  /**
   * What started the run.
   */
  readonly trigger: SynchronizationTrigger;

  /**
   * Outcome of the run.
   */
  readonly status: SynchronizationRunStatus;

  /**
   * ISO-8601 instant the run started at, or `null` when it never did.
   */
  readonly startedAt: string | null;

  /**
   * ISO-8601 instant the run finished at, or `null` while it is still in flight.
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
   * Matches imported by the run, every player included.
   */
  readonly matchesImported: number;

  /**
   * Aggregated failure description, or `null` when the run reported none.
   */
  readonly errorMessage: string | null;
}

/**
 * One synchronization run; mirrors the backend `SynchronizationResponse`.
 */
export interface SynchronizationExecution extends SynchronizationRun {
  /**
   * Instant of the last run, ISO-8601, or `null` when none ran.
   */
  readonly lastAttemptAt: string | null;

  /**
   * Instant of the last successful run, ISO-8601, or `null` when none succeeded.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;
}

/**
 * One synchronization with per-player outcomes; mirrors `SynchronizationDetailsResponse`.
 */
export interface SynchronizationDetails extends SynchronizationRun {
  /**
   * One result per player the run covered.
   */
  readonly players: readonly SynchronizationPlayerResult[];
}

/**
 * One player's outcome in a synchronization; mirrors `PlayerResultResponse`.
 */
interface SynchronizationPlayerResult {
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
  readonly status: SynchronizationRunStatus;

  /**
   * Henrik pages fetched for the player.
   */
  readonly pagesFetched: number;

  /**
   * Matches imported for this player.
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
