import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Single card of the weekly quest board: a challenge paired with its resolved visual treatment.
 */
export interface ChallengeRow {
  readonly id: number;
  readonly name: string;
  readonly description: string;

  /**
   * Difficulty tier, rendered as text beside the challenge name so the tier is not conveyed by
   * {@link visual}'s color alone (WCAG 1.4.1).
   */
  readonly difficulty: ChallengeDifficulty;

  /**
   * Damage the challenge deals to the week's boss once completed, already grouped for the active
   * language (e.g. `9 000`). The base amount, which never moves during the week.
   */
  readonly damage: string;

  /**
   * Materials one player banks for the colony by clearing it, already grouped.
   *
   * The other half of what a challenge is worth, and the half nothing used to state: the damage
   * moves the weekly ranking and the boss fight, the materials move the town's tiers. Without it the
   * campaign page asks for materials and no screen says where they come from.
   */
  readonly materials: string;

  /**
   * Squad multiplier currently applied to {@link damage}, as `"×1,2"`, or `null` while no bonus is
   * earned yet — a lone completer multiplies by one, and showing that reads as a penalty.
   */
  readonly squadMultiplier: string | null;

  /**
   * How many of the roster have already cleared it, and out of how many.
   *
   * Collective, never individual: who exactly stays `Leaderboard`'s own matrix, and the accueil's
   * confrontation band counts cleared *challenges* rather than cleared *players*. Neither of them
   * answers "how close is this one to being everyone's", which is what a squad bonus grows on.
   */
  readonly clearedCount: number;
  readonly rosterSize: number;

  /**
   * One slot per roster member, `true` for each one already cleared — the same slug device the
   * confrontation band draws turnout with, so the two read as one measure. Precomputed here rather
   * than in the template: a repeat count is not something a template should be deriving.
   */
  readonly slots: readonly boolean[];

  readonly visual: ChallengeVisual;
}

/**
 * One row of the catalogue band: a challenge the pool can still draw, outside of this week's
 * five — what it is always worth, with none of the collective-progress fields a drawn challenge
 * carries.
 *
 * Carries no visual of its own: the row lives under a tier heading that names its difficulty and
 * holds the tier's colour, so repeating the badge on every one of some fifty rows would state the
 * same thing twice.
 */
export interface CatalogueRow {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly damage: string;
  readonly materials: string;
}

/**
 * The catalogue's rows for one difficulty, in the ladder's own order.
 *
 * The pool is dozens of entries long and its only structure is the tier, which is also what decides
 * what an entry pays — so it is what the list is grouped and ordered by, rather than a column the
 * eye has to re-sort.
 */
export interface CatalogueGroup {
  readonly difficulty: ChallengeDifficulty;

  /**
   * Roman rank (`I` to `V`) and bare hex accent of the tier, from `resolveDifficultyVisual` — the
   * colour the group's heading and the leading edge of each of its rows are lit from.
   */
  readonly tier: string;
  readonly tierColor: string;

  readonly rows: readonly CatalogueRow[];
}
