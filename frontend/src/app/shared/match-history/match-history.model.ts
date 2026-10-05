import { HistoryMatch } from '@core/matches/day/match-day.model';
import { MatchScore } from '@core/matches/match.model';

/**
 * Stat column of the history, also the suffix of its `columns`/`columnHints` translation keys.
 */
export type MatchStatColumn = 'kda' | 'kd' | 'headshotPercentage' | 'adr' | 'acs';

/**
 * Colour of a stat figure: `muted` for the missing-value dash, `good`/`average` against a threshold.
 */
export type MatchStatTone = 'primary' | 'muted' | 'good' | 'average';

/**
 * Who won a match, as the row's accent and the player's score colour show it.
 */
export type MatchResultTone = 'win' | 'loss' | 'neutral';

/**
 * One formatted stat figure under its column label.
 */
export interface MatchStatCell {
  /**
   * Column the figure belongs to.
   */
  readonly column: MatchStatColumn;

  /**
   * Formatted figure, a dash when missing.
   */
  readonly value: string;

  /**
   * Colour of the figure.
   */
  readonly tone: MatchStatTone;
}

/**
 * Damage a match dealt to the guardian, with the day's ladder reduction.
 */
export interface MatchDamageCell {
  /**
   * Formatted damage amount.
   */
  readonly amount: string;

  /**
   * Whether the match dealt no damage, shown muted.
   */
  readonly isZero: boolean;

  /**
   * Translated explanation of the amount: nothing, full value, or reduced.
   */
  readonly explanation: string;

  /**
   * Kept share written beside the down arrow (`50 %`), `null` at full value or none.
   */
  readonly reducedShare: string | null;

  /**
   * Translated sentence read behind the down arrow, `null` with {@link reducedShare}.
   */
  readonly reducedShareLabel: string | null;
}

/**
 * One match row of the history, formatted for the template.
 */
export interface MatchHistoryRow {
  /**
   * The match itself.
   */
  readonly match: HistoryMatch;

  /**
   * Route to the match's detail, under the player who played it.
   */
  readonly link: readonly (string | number)[];

  /**
   * Local start time.
   */
  readonly time: string;

  /**
   * Who won, for the accent stripe and the score colour.
   */
  readonly resultTone: MatchResultTone;

  /**
   * Round score split by side, `null` when the mode reports none.
   */
  readonly score: MatchScore | null;

  /**
   * Stat figures, in column order.
   */
  readonly stats: readonly MatchStatCell[];

  /**
   * Guardian damage cell.
   */
  readonly damage: MatchDamageCell;
}

/**
 * One day of the history, formatted for the template.
 */
export interface MatchHistoryDay {
  /**
   * Day as `YYYY-MM-DD`, the tracking key.
   */
  readonly dayKey: string;

  /**
   * Formatted date.
   */
  readonly dateLabel: string;

  /**
   * Matches won that day.
   */
  readonly wins: number;

  /**
   * Matches lost that day.
   */
  readonly losses: number;

  /**
   * Day stat figures, in column order.
   */
  readonly stats: readonly MatchStatCell[];

  /**
   * Formatted total damage of the day.
   */
  readonly damage: string;

  /**
   * The day's matches.
   */
  readonly rows: readonly MatchHistoryRow[];
}
