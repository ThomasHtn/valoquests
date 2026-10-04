import { CompetitiveTier } from './competitive-tier/player-competitive-tier.model';

/**
 * Tracking status; mirrors the backend `PlayerStatus`.
 */
export type PlayerStatus = 'ACTIVE' | 'INACTIVE';

/**
 * Player summary of `GET /api/players`; mirrors the backend `PlayerSummaryResponse`.
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
   * Agent name resolving a bundled avatar, `null` when not synchronized.
   */
  readonly portrait: string | null;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the tier, `null` when not synchronized.
   */
  readonly rankRating: number | null;

  /**
   * KDA ratio, `null` when not synchronized.
   */
  readonly kda: number | null;

  /**
   * Win rate in percent (e.g. `49.4`), `null` when not synchronized.
   */
  readonly winRate: number | null;

  /**
   * Headshot rate in percent, `null` when not synchronized.
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
   * ISO-8601 instant of the last successful synchronization, `null` if none.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;
}
