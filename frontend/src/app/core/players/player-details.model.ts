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
  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Share of matches won, in percent.
   */
  readonly winRate: number;

  /**
   * Average damage per round.
   */
  readonly adr: number;

  /**
   * Average combat score.
   */
  readonly acs: number;

  /**
   * Share of hits that landed on the head, in percent.
   */
  readonly headshotPercentage: number;

  /**
   * Kills scored.
   */
  readonly kills: number;

  /**
   * Deaths suffered.
   */
  readonly deaths: number;

  /**
   * Assists given.
   */
  readonly assists: number;

  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Matches won.
   */
  readonly wins: number;

  /**
   * Loss examples at a few breakthrough levels.
   */
  readonly losses: number;

  /**
   * Matches finished as MVP.
   */
  readonly mvps: number;
}

/**
 * Detailed tracked-player profile, as exposed by `GET /api/players/{id}`.
 *
 * Mirrors the backend `PlayerDetailsResponse`. Consumed by the player-profile screen for identity,
 * rank and aggregated statistics.
 */
export interface PlayerDetails {
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
   * Aggregated figures over the filtered matches.
   */
  readonly statistics: PlayerStatistics;

  /**
   * Where the player stands on today’s ladder.
   */
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
