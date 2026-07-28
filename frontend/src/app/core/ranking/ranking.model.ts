import { CompetitiveTier } from '../players/competitive-tier.model';

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
  readonly position: number;
  readonly previousPosition: number | null;
  readonly positionVariation: number;
  readonly player: PlayerRanking;
  readonly points: number;
  readonly completedChallenges: number;
  readonly totalChallenges: number;
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
