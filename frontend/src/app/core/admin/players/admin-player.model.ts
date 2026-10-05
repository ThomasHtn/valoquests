import { PlayerStatus } from '@core/players/player-summary.model';

/**
 * Player status in the backoffice: the public `PlayerStatus` plus admin-only `ARCHIVED`.
 */
export type AdminPlayerStatus = PlayerStatus | 'ARCHIVED';

/**
 * Tracked player of `GET /api/admin/players`; mirrors the backend `PlayerAdminResponse`.
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
   * ISO-8601 instant of the last successful synchronization, `null` if none.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;

  /**
   * Whether finalized campaign data depends on this player (a deletion then archives).
   */
  readonly hasCampaignContribution: boolean;

  /**
   * Whether the player played in the last two weeks; idle players still size the guardian.
   */
  readonly hasRecentMatch: boolean;
}

/**
 * Body of `PUT /api/admin/players/{id}`: the player's identity.
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
 * Body of `POST /api/admin/players`: an identity plus the status it starts with.
 */
export interface AdminPlayerCreateRequest extends AdminPlayerUpdateRequest {
  /**
   * Status the player is created with.
   */
  readonly status: AdminPlayerStatus;
}

/**
 * What a deletion did: a player frozen into a campaign roster is archived instead.
 */
type AdminPlayerDeletionOutcome = 'DELETED' | 'ARCHIVED';

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
