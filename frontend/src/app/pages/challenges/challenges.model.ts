import { ChallengeCatalogueEntry } from '@core/challenges/challenge.model';
import { ChallengeTier } from '@core/challenges/challenge-visual.model';

/**
 * One operator's line on a challenge card: the portrait, the name, a band closing toward the
 * target and the figures over it.
 */
export interface ChallengeRung {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Resolved portrait URL, or `null` for the fallback icon.
   */
  readonly portrait: string | null;

  /**
   * Share of the target reached, in [0, 1].
   */
  readonly fraction: number;

  /**
   * Progress so far, unformatted, which the band's tooltip spells out exactly.
   */
  readonly value: number;

  /**
   * Progress so far, formatted.
   */
  readonly valueLabel: string;

  /**
   * Value to reach, formatted, or empty for an open-ended challenge.
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
 * One operator of the roster, as the cards line them up.
 */
export interface ChallengeOperator {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Resolved portrait URL, or `null` for the fallback icon.
   */
  readonly portrait: string | null;
}

/**
 * Where one operator stands on one challenge.
 */
export interface OperatorProgress {
  /**
   * Progress so far, in the challenge's own unit.
   */
  readonly value: number;

  /**
   * Whether the operator validated it.
   */
  readonly done: boolean;
}

/**
 * How a card is lit and keyed, the part of it that depends on the draw rather than the challenge.
 */
export interface ChallengeLook {
  /**
   * CSS colour the card is lit from: the tier's accent, or cyan for the day's challenge.
   */
  readonly tone: string;

  /**
   * What the hexagon carries: the tier's numeral, or the bolt of the daily draw (`D`).
   */
  readonly mark: ChallengeTier;

  /**
   * Key line above the name: the difficulty, or "daily challenge".
   */
  readonly kind: string;
}

/**
 * A challenge as one card shows it, whether the day's or one of the week's five.
 *
 * Everything is already worded: the card only lays it out, so the day's challenge and a weekly
 * one read as the same object with a different key line.
 */
export interface ChallengeCard {
  /**
   * CSS colour the card is lit from: the tier's accent, or cyan for the day's challenge.
   */
  readonly tone: string;

  /**
   * What the hexagon carries: the tier's numeral, or the bolt of the daily draw (`D`).
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
   * What has to be done, translated.
   */
  readonly description: string;

  /**
   * Wounded one operator brings back by validating it.
   */
  readonly survivors: number;

  /**
   * Ranking points it pays, what the card shows while no campaign is running.
   */
  readonly rankingPoints: number;

  /**
   * True while a campaign is running: the wounded count is then what the card shows.
   */
  readonly rescueActive: boolean;

  /**
   * Value to reach, or `null` for an open-ended challenge: the band gets a notch per unit when
   * it counts few enough of them.
   */
  readonly target: number | null;

  /**
   * One line per operator, the furthest along first.
   */
  readonly rungs: readonly ChallengeRung[];

  /**
   * Operators who validated it.
   */
  readonly doneCount: number;
}

/**
 * Where one of the seven days stands.
 */
export type DayState = 'closed' | 'now' | 'ahead';

/**
 * What the picked day is resolved from: the week on screen and its seven cells.
 */
export interface DayPickSource {
  /**
   * Monday of the week on screen, `null` outside one.
   */
  readonly weekStart: string | null;

  /**
   * The week's seven cells.
   */
  readonly days: readonly DayCell[];
}

/**
 * One cell of the seven-day strip.
 */
export interface DayCell {
  /**
   * Position in the week, Monday first.
   */
  readonly index: number;

  /**
   * Where the day stands.
   */
  readonly state: DayState;

  /**
   * Short weekday, the cell's own label (`Lun`).
   */
  readonly weekday: string;

  /**
   * Day of the month, beside the weekday (`4`).
   */
  readonly date: string;

  /**
   * Whether a challenge was drawn that day: never for a day ahead, not for a day the tick missed.
   */
  readonly drawn: boolean;

  /**
   * Operators who validated it.
   */
  readonly doneCount: number;

  /**
   * Operators on the roster.
   */
  readonly total: number;

  /**
   * The whole sentence the cell's figure abbreviates, shown on hover and read to assistive tech.
   */
  readonly tip: string;
}

/**
 * One group of the catalogue: the daily pool, or one difficulty of the weekly one.
 */
export interface CatalogueGroup {
  /**
   * Translation key suffix of the group.
   */
  readonly key: string;

  /**
   * Accent colour.
   */
  readonly tone: string;

  /**
   * Tier mark drawn on the badge.
   */
  readonly mark: ChallengeTier;

  /**
   * Translated group title.
   */
  readonly label: string;

  /**
   * Catalogue entries of the group.
   */
  readonly entries: readonly ChallengeCatalogueEntry[];
}

/**
 * Where one operator stands on one row of the board: a ring on the table, a line on a phone.
 */
export interface BoardMark extends ChallengeRung {
  /**
   * Worded progress, operator named, for the tooltip and assistive tech.
   */
  readonly tip: string;

  /**
   * The ring's figure without its compact unit (`12,7`).
   */
  readonly figure: string;

  /**
   * Compact unit following the figure (`k`), set a step smaller; empty below a thousand.
   */
  readonly unit: string;

  /**
   * One flag per unit, lit once reached, when the target counts few enough of them; empty when
   * the phone line runs continuous.
   */
  readonly segments: readonly boolean[];

  /**
   * The exact figures the hover bubble lays out.
   */
  readonly detail: MarkDetail;
}

/**
 * Where an operator stands, as a progress bubble reads it.
 */
export type MarkState = 'idle' | 'open' | 'done';

/**
 * What separates the value from the target: what remains, what exceeds it, or nothing.
 */
export type MarkGap = 'remaining' | 'surplus' | 'none';

/**
 * One operator's progress on one challenge, laid out for its hover bubble.
 */
export interface MarkDetail {
  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Accent colour of the challenge, carried because the bubble renders outside the row.
   */
  readonly tone: string;

  /**
   * Where the operator stands.
   */
  readonly state: MarkState;

  /**
   * Progress so far, exact and formatted, uncapped past the target.
   */
  readonly value: string;

  /**
   * Value to reach, formatted, or empty for an open-ended challenge.
   */
  readonly target: string;

  /**
   * Kind of gap between the value and the target.
   */
  readonly gap: MarkGap;

  /**
   * The gap, formatted, signed when it exceeds the target; empty when there is none.
   */
  readonly gapLabel: string;

  /**
   * The gap, unformatted, which picks the plural of its caption.
   */
  readonly gapCount: number;
}

/**
 * One challenge as the board lays it out: a table row, or a card on a phone.
 */
export interface BoardRow extends ChallengeCard {
  /**
   * Identity of the drawn challenge, for tracking.
   */
  readonly key: string;

  /**
   * Whether it is a day's challenge, which carries the week's tally.
   */
  readonly daily: boolean;

  /**
   * When the day's challenge closes, in epoch milliseconds; `null` for a weekly one or a closed day.
   */
  readonly closesAt: number | null;

  /**
   * One mark per operator, in the board's operator order.
   */
  readonly marks: readonly BoardMark[];
}

/**
 * One operator as the board lines them up: a column on the table, a button on a phone's squad bar.
 */
export interface BoardOperator extends ChallengeOperator {
  /**
   * What the operator's validations earned this week, dailies included: wounded while a campaign
   * runs, ranking points otherwise.
   */
  readonly reward: number;

  /**
   * Whether the reader pinned them first.
   */
  readonly pinned: boolean;

  /**
   * The sentence the footer's figures abbreviate.
   */
  readonly summary: string;
}

/**
 * One stretch of a challenge's rule: plain words, or a number ("3", "25 000").
 */
export interface RulePart {
  /**
   * The text of the stretch, as written.
   */
  readonly text: string;

  /**
   * Whether the stretch is a number, which the board sets in bold.
   */
  readonly number: boolean;
}
