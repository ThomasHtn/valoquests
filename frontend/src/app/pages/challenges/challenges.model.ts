import { ChallengeCatalogueEntry } from '@core/challenges/challenge.model';
import { ChallengeOperator } from '@core/challenges/card/challenge-card.model';
import { ChallengeTier } from '@core/challenges/visual/challenge-visual.model';

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
   * Whether a challenge was drawn that day (never ahead, not on a missed tick).
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
   * Full sentence behind the cell's figure, for hover and assistive tech.
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
 * An operator on the board: a table column, or a button on a phone's squad bar.
 */
export interface BoardOperator extends ChallengeOperator {
  /**
   * Week's earnings, dailies included: wounded while a campaign runs, ranking points otherwise.
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
