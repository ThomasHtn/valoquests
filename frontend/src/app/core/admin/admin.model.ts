import { CampaignStatus, CampaignTier, SquadLevel } from '@core/campaign/campaign.model';
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
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Riot ID game name, before the `#`.
   */
  readonly gameName: string;

  /**
   * Riot ID tag line, after the `#`.
   */
  readonly tagLine: string;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Agent name backing the bundled avatar, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Lifecycle status, archived included.
   */
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
   * Shown because an active operator who has stopped playing costs the squad: the roster size
   * sizes the guardian and the group of wounded, so an account left active and away raises both
   * without contributing to either.
   */
  readonly hasRecentMatch: boolean;
}

/**
 * Identity of a player being added to the roster, as accepted by `POST /api/admin/players`.
 */
export interface AdminPlayerCreateRequest {
  /**
   * Riot ID game name, before the `#`.
   */
  readonly gameName: string;

  /**
   * Riot ID tag line, after the `#`.
   */
  readonly tagLine: string;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Status the player is created with.
   */
  readonly status: AdminPlayerStatus;
}

/**
 * Editable identity of a tracked player, as accepted by `PUT /api/admin/players/{id}`.
 */
export interface AdminPlayerUpdateRequest {
  /**
   * Riot ID game name, before the `#`.
   */
  readonly gameName: string;

  /**
   * Riot ID tag line, after the `#`.
   */
  readonly tagLine: string;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;
}

/**
 * What a deletion request did to a player.
 *
 * Not predictable by the caller: a player frozen into a campaign's roster is archived rather than
 * deleted, so the screen only learns which happened from the response.
 */
export type AdminPlayerDeletionOutcome = 'DELETED' | 'ARCHIVED';

/**
 * Outcome of `DELETE /api/admin/players/{id}`.
 */
export interface AdminPlayerDeletionResult {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * What the deletion did.
   */
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
 * One player's outcome within a synchronization execution.
 *
 * Mirrors the backend `SynchronizationDetailsResponse.PlayerResultResponse`.
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
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Ordinal of the campaign, first one being 1.
   */
  readonly number: number;

  /**
   * Lifecycle status of the campaign.
   */
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

  /**
   * Reference figure the squad was calibrated on.
   */
  readonly reference: number;

  /**
   * Tier the squad was measured at.
   */
  readonly tier: CampaignTier;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;
}

/**
 * One operator's share of the squad's calibration.
 *
 * Mirrors the backend `PlayerCalibration`.
 */
export interface PlayerCalibration {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Average weekly output over the window, in guardian damage.
   */
  readonly weeklyAverage: number;

  /**
   * Weeks of history the calibration counted.
   */
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
  /**
   * Reference figure the squad was calibrated on.
   */
  readonly reference: number;

  /**
   * Tier the squad was measured at.
   */
  readonly tier: CampaignTier;

  /**
   * Which of the catalogue's two grids the campaign would play.
   */
  readonly level: SquadLevel;

  /**
   * Months of history the calibration read.
   */
  readonly windowMonths: number;

  /**
   * First day of the window, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly firstDay: string;

  /**
   * One calibration line per player.
   */
  readonly players: readonly PlayerCalibration[];
}
