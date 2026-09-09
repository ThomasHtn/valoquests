import { CompetitiveTier } from './competitive-tier.model';

/**
 * Tracking status of a player, as exposed by `GET /api/players`.
 *
 * Mirrors the backend `PlayerStatus` enum.
 */
export type PlayerStatus = 'ACTIVE' | 'INACTIVE';

/**
 * Compact tracked-player summary, as exposed by `GET /api/players`.
 *
 * Mirrors the backend `PlayerSummaryResponse`. Consumed by the sidebar to resolve the global last
 * synchronization timestamp, and by the players list screen for identity and statistics fields.
 */
export interface PlayerSummary {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Full Riot ID, `gameName#tagLine`.
   */
  readonly riotId: string;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Name of the player's associated agent, used to resolve a bundled avatar, or `null` when not
   * yet synchronized.
   */
  readonly portrait: string | null;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the player's current tier, or `null` when not yet synchronized.
   */
  readonly rankRating: number | null;

  /**
   * KDA ratio, or `null` when not yet synchronized.
   */
  readonly kda: number | null;

  /**
   * Win rate as a percentage (e.g. `49.4`), or `null` when not yet synchronized.
   */
  readonly winRate: number | null;

  /**
   * Headshot rate as a percentage, or `null` when not yet synchronized.
   */
  readonly headshotPercentage: number | null;

  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Whether the player is active or paused.
   */
  readonly status: PlayerStatus;

  /**
   * Instant of the player's last successful synchronization, as an ISO-8601 instant, or `null`
   * when the player has never been synchronized successfully.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;
}
