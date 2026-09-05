/**
 * Difficulty tier of a weekly challenge, controlling its weight. Mirrors the backend
 * `ChallengeDifficulty`. A daily challenge has none.
 */
export type ChallengeDifficulty = 'EASY' | 'NORMAL' | 'MEDIUM' | 'HARD' | 'VERY_HARD';

/**
 * The five tiers from easiest to hardest — the order any list of difficulties is ordered by.
 *
 * Declared here rather than relied on through the key order of a lookup record: the ladder is a
 * domain fact, and object key order is not a contract.
 */
export const CHALLENGE_DIFFICULTIES: readonly ChallengeDifficulty[] = [
  'EASY',
  'NORMAL',
  'MEDIUM',
  'HARD',
  'VERY_HARD',
];

/**
 * How often a challenge is drawn. Mirrors the backend `ChallengeCadence`.
 *
 * `WEEKLY` is one of the five drawn on Monday, decided over the week; `DAILY` is the one drawn
 * every morning, decided inside its own day.
 */
export type ChallengeCadence = 'WEEKLY' | 'DAILY';

/**
 * What every challenge carries, whether drawn or read from the catalogue.
 *
 * Targets come already resolved: the backend scales them at the draw from the campaign's own
 * measure of the squad, so the figure shown is the figure that counts. A description names the
 * catalogue's base figure, which is why a screen prints {@link targetValue} rather than trusting
 * the description alone.
 */
export interface ChallengeIdentity {
  readonly id: number;

  /**
   * Stable catalogue code (`EASY_DM_HEADSHOTS`), the key to any per-challenge visual.
   */
  readonly code: string;
  readonly name: string;
  readonly description: string;
  readonly cadence: ChallengeCadence;

  /**
   * Difficulty of a weekly challenge, `null` for a daily one.
   */
  readonly difficulty: ChallengeDifficulty | null;

  /**
   * Whether only competitive matches count toward it.
   */
  readonly competitiveOnly: boolean;

  /**
   * Metric(s) evaluated, joined with `" + "` for composite challenges (`"KILLS + MATCHES_PLAYED"`).
   */
  readonly metric: string;

  /**
   * Resolved target, or `null` for a composite challenge with no single stored target.
   */
  readonly targetValue: number | null;

  /**
   * Survivors one operator rescues by validating it, at the week it was drawn for.
   */
  readonly survivors: number;

  /**
   * Points it adds to the weekly ranking once validated.
   */
  readonly rankingPoints: number;
}

/**
 * Collective progress of a challenge drawn for the current week or day.
 *
 * Mirrors `CurrentChallengesResponse.ChallengeProgressResponse` from the backend. Progress is
 * collective (across the squad): individual progress is only available from the ranking.
 */
export interface ChallengeProgress extends ChallengeIdentity {
  /**
   * Day a daily challenge is decided on, as an ISO-8601 date (`YYYY-MM-DD`); `null` for a weekly.
   */
  readonly day: string | null;
  readonly completedPlayers: number;
  readonly totalPlayers: number;

  /**
   * Identifiers of the active operators who validated it, matched against
   * {@link CurrentChallenges.roster}.
   */
  readonly completedPlayerIds: readonly number[];
  readonly completionPercentage: number;
}

/**
 * One active operator, the unit every completion count is read against.
 *
 * Mirrors `CurrentChallengesResponse.RosterPlayerResponse` from the backend.
 */
export interface RosterPlayer {
  readonly id: number;
  readonly displayName: string;
}

/**
 * Challenges drawn for the active calendar week, with their collective completion progress.
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
   * The day in progress, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly today: string;

  /**
   * Most recent successful player synchronization, as an ISO-8601 instant, or `null` when none
   * completed yet.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;

  /**
   * The active operators every challenge applies to, in roster order.
   */
  readonly roster: readonly RosterPlayer[];

  /**
   * The week's five, one per difficulty, easiest first.
   */
  readonly challenges: readonly ChallengeProgress[];

  /**
   * The week's daily challenges drawn so far, oldest first. Today's is the last one.
   */
  readonly dailies: readonly ChallengeProgress[];
}

/**
 * One challenge of the catalogue, outside of any one week's draw — what it is worth rather than
 * how far the squad has got with it.
 *
 * Mirrors the backend `ChallengeCatalogueResponse.ChallengeCatalogueEntry` returned by
 * `GET /api/challenges/catalogue`.
 */
export type ChallengeCatalogueEntry = ChallengeIdentity;

/**
 * The full catalogue of challenges the draws pick from, priced at the reference in force.
 *
 * Mirrors the backend `ChallengeCatalogueResponse` returned by `GET /api/challenges/catalogue`.
 */
export interface ChallengeCatalogue {
  /**
   * Weekly reference the targets and rewards are resolved at: the live campaign's, else the last
   * closed one's, else the floor.
   */
  readonly reference: number;
  readonly challenges: readonly ChallengeCatalogueEntry[];
}
