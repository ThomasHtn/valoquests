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
  readonly dailyYield: DailyYield;
}

/**
 * Where a player stands on today's diminishing-returns ladder, before their next match.
 *
 * Mirrors the backend `PlayerDetailsResponse.DailyYield`. The rule that turns "play more" into "play
 * more often" was only ever stated after the fact — a match carried the share it had already kept —
 * so a player learned it by losing value to it. This is the same rule read forwards.
 */
export interface DailyYield {
  /**
   * Valued matches already played today, in any mode.
   */
  readonly matchesToday: number;

  /**
   * Share of its base damage the next match would keep, from 0 to 100.
   */
  readonly nextMatchPercent: number;

  /**
   * Rank at which the share falls further, or `null` once the ladder has bottomed out and nothing
   * falls again.
   */
  readonly dropsAtRank: number | null;

  /**
   * Share kept from {@link dropsAtRank} on, or `null` at the floor.
   */
  readonly dropsToPercent: number | null;
}
