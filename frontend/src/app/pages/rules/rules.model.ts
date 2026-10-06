import { CampaignDifficulty } from '@core/campaign/campaign.model';
import { GuardianCategory } from '@core/campaign/campaign-week.model';
import { ChallengeTier } from '@core/challenges/challenge.model';

/**
 * Icon of a closing-sheet constant.
 */
export type RuleConstantIcon = 'sync' | 'base' | 'food' | 'components' | 'bed';

/**
 * Colour of a closing-sheet icon: brand, or the resource it concerns.
 */
export type RuleConstantTone = 'brand' | 'food' | 'components';

/**
 * One value of the closing sheet: its dictionary key and how its label is marked.
 */
export interface RuleConstant {
  /**
   * Key under `rules.sections.constants.items`, holding the label and the value.
   */
  readonly key: string;

  /**
   * Icon set before the label.
   */
  readonly icon: RuleConstantIcon;

  /**
   * Colour of the icon.
   */
  readonly tone: RuleConstantTone;
}

/**
 * What a match is worth by mode and outcome.
 */
export interface MatchDamageRow {
  /**
   * Game mode key.
   */
  readonly key: string;

  /**
   * Worth of a loss.
   */
  readonly loss: number;

  /**
   * Worth of a draw, `null` when the mode has none.
   */
  readonly draw: number | null;

  /**
   * Worth of a win.
   */
  readonly win: number;
}

/**
 * Modes sharing one food/components split.
 */
export interface ModeGroup {
  /**
   * Group key (`long`, `short`).
   */
  readonly key: string;

  /**
   * Share of the worth paid as food, in percent.
   */
  readonly foodPercent: number;

  /**
   * Modes of the group.
   */
  readonly modes: readonly MatchDamageRow[];
}

/**
 * A step of a ladder: a label key and the percentage it applies.
 */
export interface LadderStep {
  /**
   * Label key.
   */
  readonly key: string;

  /**
   * Percentage applied.
   */
  readonly percent: number;
}

/**
 * Streak bonus, by days played in the week; the last step is open-ended.
 */
export interface StreakStep {
  /**
   * Days played in the week.
   */
  readonly days: number;

  /**
   * Bonus, in percent.
   */
  readonly percent: number;

  /**
   * Whether the step also covers every day past it.
   */
  readonly open: boolean;
}

/**
 * Sunday's worked example, one line per figure, on a group of forty wounded.
 */
export interface SundayExampleRow {
  /**
   * Label key.
   */
  readonly key: string;

  /**
   * Figure, already written out.
   */
  readonly value: string;

  /**
   * Whether the line is a total.
   */
  readonly emphasised: boolean;
}

/**
 * What a surviving guardian takes from the base, by breach reached.
 */
export interface LossStep {
  /**
   * Breach reached, in percent.
   */
  readonly breach: number;

  /**
   * Share of the population lost, in percent.
   */
  readonly percent: number;
}

/**
 * A campaign week's shape, weights in shares of the reference; `how` flags weeks worth a note.
 */
export interface CampaignWeekShape {
  /**
   * Guardian category.
   */
  readonly category: GuardianCategory;

  /**
   * Guardian hit points, as a share of the reference.
   */
  readonly guardian: number;

  /**
   * Wounded group size, as a share of the reference.
   */
  readonly group: number;

  /**
   * Whether the week gets an explanatory note.
   */
  readonly how: boolean;
}

/**
 * One difficulty, with the reference it carries.
 */
export interface DifficultyBand {
  /**
   * Difficulty.
   */
  readonly key: CampaignDifficulty;

  /**
   * Reference value of the difficulty.
   */
  readonly reference: number;
}

/**
 * What a challenge is worth, by cadence and difficulty, at the example reference.
 */
export interface ChallengeWorth {
  /**
   * Difficulty, `null` for the daily challenge.
   */
  readonly difficulty: ChallengeTier | null;

  /**
   * Weight against the reference.
   */
  readonly weight: number;

  /**
   * Wounded brought home at the example reference.
   */
  readonly survivors: number;
}
