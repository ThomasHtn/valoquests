/**
 * Difficulty tier of a challenge, controlling its point reward.
 */
export type ChallengeDifficulty = 'EASY' | 'NORMAL' | 'MEDIUM' | 'HARD' | 'VERY_HARD';

/**
 * Collective progress of a single challenge selected for the current week.
 *
 * Mirrors `CurrentChallengesResponse.ChallengeProgressResponse` from the backend. Progress is
 * collective (across all tracked players) rather than per-player: individual progress is only
 * available from the ranking endpoint.
 */
export interface ChallengeProgress {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly difficulty: ChallengeDifficulty;

  /**
   * Metric(s) evaluated by this challenge, joined with `" + "` for composite challenges
   * (e.g. `"HEADSHOTS"` or `"KILLS + MATCHES_PLAYED"`).
   */
  readonly metric: string;

  /**
   * Target value for the challenge, or `null` for a composite challenge with no stored progress.
   */
  readonly targetValue: number | null;
  readonly points: number;
  readonly completedPlayers: number;
  readonly totalPlayers: number;
  readonly completionPercentage: number;
}

/**
 * Challenges selected for the active calendar week, with their collective completion progress.
 *
 * Mirrors the backend `CurrentChallengesResponse` returned by `GET /api/challenges/current`.
 */
export interface CurrentChallenges {
  /**
   * Monday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Most recent successful player synchronization, as an ISO-8601 instant, or `null` when none
   * completed yet.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;
  readonly challenges: readonly ChallengeProgress[];
}
