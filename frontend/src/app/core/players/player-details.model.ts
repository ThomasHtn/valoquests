import { CompetitiveTier } from './competitive-tier.model';

/**
 * Aggregated performance statistics computed from a player's entire match history, as exposed by
 * `GET /api/players/{id}`.
 *
 * Mirrors the backend `PlayerDetailsResponse.PlayerStatistics` record. Unlike {@link PlayerSummary},
 * these are always numeric (defaulting to `0` when the player has no recorded match) since they are
 * recomputed from match history rather than read from a cached, possibly-unset field.
 */
export interface PlayerStatistics {
  readonly kda: number;
  readonly winRate: number;
  readonly adr: number;
  readonly acs: number;
  readonly headshotPercentage: number;
  readonly kills: number;
  readonly deaths: number;
  readonly assists: number;
  readonly matchesPlayed: number;
  readonly wins: number;
  readonly losses: number;
  readonly mvps: number;
}

/**
 * Detailed tracked-player profile, as exposed by `GET /api/players/{id}`.
 *
 * Mirrors the backend `PlayerDetailsResponse`. Consumed by the player-profile screen for identity,
 * rank and aggregated statistics.
 */
export interface PlayerDetails {
  readonly id: number;
  readonly riotId: string;
  readonly displayName: string;

  /**
   * Name of the player's associated agent, used to resolve a bundled avatar, or `null` when not
   * yet synchronized.
   */
  readonly portrait: string | null;
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the player's current tier, or `null` when not yet synchronized.
   */
  readonly rankRating: number | null;
  readonly statistics: PlayerStatistics;
}
