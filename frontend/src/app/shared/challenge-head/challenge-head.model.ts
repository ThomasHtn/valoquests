import type { LucideIcon } from '@lucide/angular';

/**
 * What completing a challenge pays, as the head shows it.
 */
export interface ChallengeHeadGain {
  /**
   * Icon of the paid concept: wounded survivors or ranking points.
   */
  readonly icon: LucideIcon;

  /**
   * Translation key of the tooltip explaining the gain.
   */
  readonly tooltipKey: string;

  /**
   * Amount paid.
   */
  readonly count: number;
}
