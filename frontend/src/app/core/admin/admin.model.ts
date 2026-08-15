import { PlayerStatus } from '@core/players/player-summary.model';

/**
 * Lifecycle status a player can hold in the backoffice.
 *
 * Widens the public {@link PlayerStatus} with the one value `GET /api/players` never returns: an
 * archived player has left the roster and only remains stored so the finalized weeks naming it stay
 * readable, which is a state only the administration screens ever see.
 */
export type AdminPlayerStatus = PlayerStatus | 'ARCHIVED';

/**
 * One tracked player, as exposed by `GET /api/admin/players`.
 *
 * Mirrors the backend `PlayerAdminResponse`. Distinct from `PlayerSummary`: administration edits
 * identities rather than displaying performance, so it carries the raw Riot fields and none of the
 * aggregated statistics.
 */
export interface AdminPlayer {
  readonly id: number;
  readonly gameName: string;
  readonly tagLine: string;
  readonly displayName: string;

  /**
   * Agent name backing the bundled avatar, or `null` when none was chosen.
   */
  readonly portrait: string | null;
  readonly status: AdminPlayerStatus;

  /**
   * Riot account identifier, or `null` until a synchronization resolves it.
   */
  readonly riotPuuid: string | null;

  /**
   * Instant of the player's last successful synchronization, as an ISO-8601 instant, or `null`
   * when it has never been synchronized successfully.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;

  /**
   * Whether finalized campaign data depends on this player.
   *
   * Decides what a deletion request will actually do, which is why the screen shows it before the
   * operator asks for one.
   */
  readonly hasCampaignContribution: boolean;
}

/**
 * Identity of a player being added to the roster, as accepted by `POST /api/admin/players`.
 */
export interface AdminPlayerCreateRequest {
  readonly gameName: string;
  readonly tagLine: string;
  readonly displayName: string;
  readonly portrait: string | null;
  readonly status: AdminPlayerStatus;
}

/**
 * Editable identity of a tracked player, as accepted by `PUT /api/admin/players/{id}`.
 */
export interface AdminPlayerUpdateRequest {
  readonly gameName: string;
  readonly tagLine: string;
  readonly displayName: string;
  readonly portrait: string | null;
}

/**
 * What a deletion request did to a player.
 *
 * Not predictable by the caller: a player that fought a boss is archived rather than deleted, so
 * the screen only learns which happened from the response.
 */
export type AdminPlayerDeletionOutcome = 'DELETED' | 'ARCHIVED';

/**
 * Outcome of `DELETE /api/admin/players/{id}`.
 */
export interface AdminPlayerDeletionResult {
  readonly playerId: number;
  readonly outcome: AdminPlayerDeletionOutcome;
}

/**
 * Lifecycle state of a synchronization execution.
 *
 * Mirrors the backend `SynchronizationStatus`. `PENDING` and `RUNNING` are the two the backoffice
 * polls on; the rest are terminal.
 */
export type SynchronizationStatus =
  'PENDING' | 'RUNNING' | 'PARTIAL' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

/**
 * One synchronization execution, as exposed by `GET /api/admin/synchronizations/latest`.
 *
 * Mirrors the backend `SynchronizationResponse`. This is the only window the backoffice has on a
 * run: the command routes answer `202` and the walk outlives the request that started it.
 */
export interface SynchronizationExecution {
  readonly id: number;
  readonly type: string;

  /**
   * Whether the run was started by hand or by the scheduler.
   */
  readonly trigger: 'SCHEDULED' | 'MANUAL';
  readonly status: SynchronizationStatus;

  /**
   * ISO-8601 instant the run started at, or `null` when it never did.
   */
  readonly startedAt: string | null;

  /**
   * ISO-8601 instant the run finished at, or `null` while it is still in flight.
   */
  readonly finishedAt: string | null;
  readonly lastAttemptAt: string | null;
  readonly lastSuccessfulSynchronizationAt: string | null;
  readonly playersProcessed: number;
  readonly failureCount: number;
  readonly matchesImported: number;

  /**
   * Aggregated failure description, or `null` when the run reported none.
   */
  readonly errorMessage: string | null;
}

/**
 * Statuses meaning a synchronization is still in flight.
 */
export const IN_FLIGHT_SYNCHRONIZATION_STATUSES: readonly SynchronizationStatus[] = [
  'PENDING',
  'RUNNING',
];
