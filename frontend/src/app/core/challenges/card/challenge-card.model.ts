import { ChallengeTier } from '../visual/challenge-visual.model';

/**
 * One operator's line on a challenge card.
 */
export interface ChallengeRung {
  /**
   * Internal player identifier.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Portrait URL, `null` for the fallback icon.
   */
  readonly portrait: string | null;

  /**
   * Share of the target reached, in [0, 1].
   */
  readonly fraction: number;

  /**
   * Raw progress, spelled out exactly by the tooltip.
   */
  readonly value: number;

  /**
   * Formatted progress.
   */
  readonly valueLabel: string;

  /**
   * Formatted target, empty for an open-ended challenge.
   */
  readonly targetLabel: string;

  /**
   * Whether the operator validated it.
   */
  readonly done: boolean;

  /**
   * Whether the operator has not started it yet.
   */
  readonly idle: boolean;
}

/**
 * One roster operator, as the cards line them up.
 */
export interface ChallengeOperator {
  /**
   * Internal player identifier.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Portrait URL, `null` for the fallback icon.
   */
  readonly portrait: string | null;
}

/**
 * Where one operator stands on one challenge.
 */
export interface OperatorProgress {
  /**
   * Progress, in the challenge's own unit.
   */
  readonly value: number;

  /**
   * Whether the operator validated it.
   */
  readonly done: boolean;
}

/**
 * The part of a card's look that depends on the draw rather than the challenge.
 */
export interface ChallengeLook {
  /**
   * CSS accent: the tier's, or cyan for the day's challenge.
   */
  readonly tone: string;

  /**
   * Hexagon content: the tier numeral, or `D` for the daily bolt.
   */
  readonly mark: ChallengeTier;

  /**
   * Key line above the name: the difficulty, or "daily challenge".
   */
  readonly kind: string;
}

/**
 * A fully worded challenge card, daily or weekly alike.
 */
export interface ChallengeCard {
  /**
   * CSS accent: the tier's, or cyan for the day's challenge.
   */
  readonly tone: string;

  /**
   * Hexagon content: the tier numeral, or `D` for the daily bolt.
   */
  readonly mark: ChallengeTier;

  /**
   * Key line above the name: the difficulty, or "daily challenge".
   */
  readonly kind: string;

  /**
   * Translated name.
   */
  readonly name: string;

  /**
   * Translated description.
   */
  readonly description: string;

  /**
   * Wounded one operator brings back by validating it.
   */
  readonly survivors: number;

  /**
   * Ranking points paid, shown while no campaign runs.
   */
  readonly rankingPoints: number;

  /**
   * Whether a campaign runs, so the card shows wounded instead of points.
   */
  readonly rescueActive: boolean;

  /**
   * Target, `null` for an open-ended challenge; small targets get one notch per unit.
   */
  readonly target: number | null;

  /**
   * One line per operator, furthest along first.
   */
  readonly rungs: readonly ChallengeRung[];

  /**
   * Operators who validated it.
   */
  readonly doneCount: number;
}

/**
 * One operator on one board row: a ring on the table, a line on a phone.
 */
export interface BoardMark extends ChallengeRung {
  /**
   * Worded progress with the operator's name, for the tooltip and assistive tech.
   */
  readonly tip: string;

  /**
   * Ring figure without its compact unit (`12,7`).
   */
  readonly figure: string;

  /**
   * Compact unit after the figure (`k`), empty below a thousand.
   */
  readonly unit: string;

  /**
   * One lit-once-reached flag per unit for small targets, empty for a continuous line.
   */
  readonly segments: readonly boolean[];

  /**
   * Exact figures for the hover bubble.
   */
  readonly detail: MarkDetail;
}

/**
 * Operator state as a progress bubble reads it.
 */
export type MarkState = 'idle' | 'open' | 'done';

/**
 * What separates the value from the target.
 */
export type MarkGap = 'remaining' | 'surplus' | 'none';

/**
 * One operator's progress on one challenge, for its hover bubble.
 */
export interface MarkDetail {
  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Challenge accent, carried because the bubble renders outside the row.
   */
  readonly tone: string;

  /**
   * Operator state.
   */
  readonly state: MarkState;

  /**
   * Exact formatted progress, uncapped past the target.
   */
  readonly value: string;

  /**
   * Formatted target, empty for an open-ended challenge.
   */
  readonly target: string;

  /**
   * Kind of gap between value and target.
   */
  readonly gap: MarkGap;

  /**
   * Formatted gap, signed when over the target, empty when none.
   */
  readonly gapLabel: string;

  /**
   * Raw gap, picks the caption's plural.
   */
  readonly gapCount: number;
}

/**
 * One challenge on the board: a table row, or a card on a phone.
 */
export interface BoardRow extends ChallengeCard {
  /**
   * Drawn challenge identity, for tracking.
   */
  readonly key: string;

  /**
   * Whether it is a day's challenge, which carries the week's tally.
   */
  readonly daily: boolean;

  /**
   * Daily close time in epoch ms, `null` for a weekly one or a closed day.
   */
  readonly closesAt: number | null;

  /**
   * One mark per operator, in board operator order.
   */
  readonly marks: readonly BoardMark[];
}
