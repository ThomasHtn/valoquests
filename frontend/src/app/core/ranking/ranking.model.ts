import { CompetitiveTier } from '@core/players/competitive-tier.model';

/**
 * Player identity and rank data attached to a ranking entry.
 *
 * Mirrors `CurrentRankingResponse.PlayerRankingResponse` from the backend.
 */
export interface PlayerRanking {
  readonly id: number;
  readonly displayName: string;

  /**
   * Relative path or URL of the player portrait, or `null` when not yet synchronized.
   */
  readonly portrait: string | null;
  readonly competitiveTier: CompetitiveTier;
  readonly rankRating: number | null;
}

/**
 * A player's exact progress toward a single challenge selected for the active week.
 *
 * Mirrors `CurrentRankingResponse.ChallengeProgressResponse` from the backend.
 */
export interface RankingChallengeProgress {
  readonly challengeId: number;
  readonly challengeName: string;
  readonly metric: string;
  readonly currentValue: number;
  readonly targetValue: number | null;
  readonly unit: string;
  readonly completed: boolean;
}

/**
 * Single row of the current weekly ranking: a player's position, score and exact progress toward
 * every challenge selected for the active week.
 *
 * Mirrors `CurrentRankingResponse.RankingEntryResponse` from the backend.
 */
export interface RankingEntry {
  /**
   * 1-based ranking position, or `null` when the player is inactive and therefore never
   * consumes a ranking slot.
   */
  readonly position: number | null;
  readonly previousPosition: number | null;
  readonly positionVariation: number;
  readonly player: PlayerRanking;
  readonly challengeDamage: number;
  readonly completedChallenges: number;
  readonly totalChallenges: number;

  /**
   * Damage dealt by the matches that counted this week, outside of any challenge.
   */
  readonly matchDamage: number;

  /**
   * Bonus earned for the number of distinct days the player was active this week.
   */
  readonly regularityBonus: number;

  /**
   * Sum of the per-challenge bonuses earned because enough of the squad cleared the same
   * challenge.
   */
  readonly teamBonus: number;
  readonly activeDays: number;

  /**
   * Damage dealt to the week's boss: {@link challengeDamage} + {@link matchDamage} +
   * {@link regularityBonus} + {@link teamBonus}. This is what the ranking is ordered on, so it is
   * the amount shown next to a position.
   */
  readonly totalDamage: number;
  readonly challengeProgress: readonly RankingChallengeProgress[];
}

/**
 * Current weekly ranking, with each player's exact progress toward every active challenge.
 *
 * Mirrors the backend `CurrentRankingResponse` returned by `GET /api/rankings/current`.
 */
export interface CurrentRanking {
  /**
   * Monday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Instant at which this ranking was last calculated, as an ISO-8601 instant.
   */
  readonly calculatedAt: string;
  readonly ranking: readonly RankingEntry[];
}

/**
 * Single player's finalized result within one historical week.
 *
 * Mirrors `RankingHistoryWeekResponse.FinalRankingEntryResponse` from the backend.
 */
export interface RankingHistoryEntry {
  readonly position: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly challengeDamage: number;
  readonly completedChallenges: number;
}

/**
 * Finalized, immutable ranking for one completed calendar week.
 *
 * Mirrors `RankingHistoryWeekResponse` from the backend.
 */
export interface RankingHistoryWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;
  readonly ranking: readonly RankingHistoryEntry[];
}
