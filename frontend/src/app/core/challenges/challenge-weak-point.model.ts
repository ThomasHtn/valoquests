import { ChallengeDifficulty } from './challenge.model';

/**
 * One of the active week's challenges, compressed to what a boss card needs of it.
 *
 * A boss's health bar states how much of it is left but nothing about how it is drained. This is a
 * challenge card reduced to its tier mark and its damage — enough to say "this is what hurts it",
 * with the quest board one click away for what each tier actually asks for. Shared by every card
 * that frames the active week's challenges as the boss's weak points (`BossEncounter`, `BossDetail`).
 */
export interface ChallengeWeakPoint {
  /**
   * Challenge identifier, used as the list's tracking key.
   */
  readonly id: number;

  /**
   * Roman numeral of the difficulty tier, `I` through `V`, as the quest board writes it.
   */
  readonly tier: string;

  /**
   * Text color utility of the numeral.
   */
  readonly iconClass: string;

  /**
   * Background color utility of the hexagon behind the numeral.
   */
  readonly barClass: string;

  /**
   * The tier's accent as a bare color, for a caller lighting the whole mark — fill, inner ground and
   * numeral — from one custom property rather than from a class per element.
   */
  readonly tierColor: string;

  /**
   * Difficulty the numeral stands for, read by assistive technology in its place.
   */
  readonly difficulty: ChallengeDifficulty;

  /**
   * Damage clearing this challenge deals to the boss, grouped in the active language.
   */
  readonly damageLabel: string;
}
