import { CampaignStatus, CampaignTier } from '@core/campaign/campaign.model';
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

  /**
   * Whether the player has played at all in the last two weeks.
   *
   * Shown because an active player who has stopped playing costs the colony real points: the roster
   * size drives the turnout denominator, the opening housing and both sides of the weekly fight at
   * once, so an account left active and away widens the town without feeding it. Roughly five
   * percent of a run's final score, which nobody would guess from this screen.
   */
  readonly hasRecentMatch: boolean;
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

/**
 * One synchronization execution and its per-player outcomes, as exposed by
 * `GET /api/admin/synchronizations/{id}`.
 *
 * Mirrors the backend `SynchronizationDetailsResponse`. A shorter reading of
 * {@link SynchronizationExecution}, plus the one thing a summary row cannot show: which player
 * failed and why.
 */
export interface SynchronizationDetails {
  readonly id: number;
  readonly type: string;
  readonly trigger: 'SCHEDULED' | 'MANUAL';
  readonly status: SynchronizationStatus;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly playersProcessed: number;
  readonly failureCount: number;
  readonly matchesImported: number;
  readonly errorMessage: string | null;
  readonly players: readonly SynchronizationPlayerResult[];
}

/**
 * One player's outcome within a synchronization execution.
 *
 * Mirrors the backend `SynchronizationDetailsResponse.PlayerResultResponse`.
 */
export interface SynchronizationPlayerResult {
  readonly playerId: number;
  readonly displayName: string;
  readonly status: SynchronizationStatus;
  readonly pagesFetched: number;
  readonly matchesImported: number;
  readonly errorMessage: string | null;

  /**
   * What ended the player's match-history walk, or `null` when they failed before completing
   * one — the one thing that explains a suspiciously short import without reading the logs.
   */
  readonly stopReason: string | null;
}

/**
 * One campaign as the backoffice sees it, returned by the opening and stopping commands.
 *
 * Mirrors the backend `CampaignAdminResponse`.
 */
export interface CampaignAdmin {
  readonly number: number;
  readonly status: CampaignStatus;

  /**
   * Monday of the first week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly firstWeekStart: string;

  /**
   * Monday of the tenth week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly lastWeekStart: string;

  /**
   * Day the campaign was frozen on when stopped early, or `null`.
   */
  readonly stoppedOn: string | null;
  readonly reference: number;
  readonly tier: CampaignTier;
  readonly rosterSize: number;
}

/**
 * One operator's share of the squad's calibration.
 *
 * Mirrors the backend `PlayerCalibration`.
 */
export interface PlayerCalibration {
  readonly playerId: number;
  readonly displayName: string;

  /**
   * Average weekly output over the window, in guardian damage.
   */
  readonly weeklyAverage: number;
  readonly weeksCounted: number;

  /**
   * First day a match is known for, as an ISO-8601 date, or `null` with no history at all.
   */
  readonly earliestMatchDay: string | null;

  /**
   * Whether the imported history reaches back to the start of the window.
   */
  readonly covered: boolean;

  /**
   * Whether the operator has under a month of history and takes the squad's median instead.
   */
  readonly beginner: boolean;
}

/**
 * The measure a campaign opened today would be given, from `GET /api/admin/campaigns/calibration`.
 *
 * Mirrors the backend `SquadCalibrationResponse`.
 */
export interface SquadCalibration {
  readonly reference: number;
  readonly tier: CampaignTier;
  readonly volumeFactor: number;
  readonly windowMonths: number;

  /**
   * First day of the window, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly firstDay: string;
  readonly players: readonly PlayerCalibration[];
}
