import { CompetitiveTier } from './competitive-tier/player-competitive-tier.model';

/**
 * Statistics over the whole match history, `0` without matches; mirrors `PlayerStatistics`.
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
   * Matches lost.
   */
  readonly losses: number;

  /**
   * Matches finished as MVP.
   */
  readonly mvps: number;
}

/**
 * Player profile of `GET /api/players/{id}`; mirrors the backend `PlayerDetailsResponse`.
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
   * Aggregated figures over the filtered matches.
   */
  readonly statistics: PlayerStatistics;

  /**
   * Where the player stands on today’s ladder.
   */
  readonly dailyYield: DailyYield;
}

/**
 * Today's diminishing-returns ladder before the next match; mirrors the backend `DailyYield`.
 */
interface DailyYield {
  /**
   * Valued matches already played today, in any mode.
   */
  readonly matchesToday: number;

  /**
   * Share of its base damage the next match would keep, from 0 to 100.
   */
  readonly nextMatchPercent: number;

  /**
   * Rank at which the share falls further, `null` at the floor.
   */
  readonly dropsAtRank: number | null;

  /**
   * Share kept from {@link dropsAtRank} on, or `null` at the floor.
   */
  readonly dropsToPercent: number | null;
}
