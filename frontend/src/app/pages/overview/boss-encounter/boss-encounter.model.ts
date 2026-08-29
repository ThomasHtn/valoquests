import { ChallengeDifficulty } from '@core/challenges/challenge.model';

/**
 * One of the week's five challenges, compressed to what the boss card needs of it.
 *
 * The health bar states how much of the boss is left but nothing about how it is drained. This is a
 * challenge card reduced to its tier mark and its damage — enough to say "this is what hurts it",
 * with the quest board one click away for what each tier actually asks for.
 */
export interface BossWeakPoint {
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
   * Difficulty the numeral stands for, read by assistive technology in its place.
   */
  readonly difficulty: ChallengeDifficulty;

  /**
   * Damage clearing this challenge deals to the boss, grouped in the active language.
   */
  readonly damageLabel: string;
}
