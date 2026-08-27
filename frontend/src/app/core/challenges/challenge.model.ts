/**
 * Difficulty tier of a challenge, controlling its damage reward.
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

  /**
   * Base damage the challenge deals once completed, before the squad bonus.
   */
  readonly damage: number;

  /**
   * Materials one player banks for the colony by validating it.
   *
   * The other half of what a challenge is worth: the damage moves the weekly ranking and the boss
   * fight, the materials move the town. Derived by the backend from that same damage, so the two can
   * never disagree.
   */
  readonly materials: number;

  /**
   * Squad bonus currently earned, as a percentage of {@link damage}, resolved by the backend from
   * the week's own scoring ruleset. Grows as teammates complete the same challenge, and applies
   * retroactively to everyone who already had.
   */
  readonly teamBonusPercent: number;
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
