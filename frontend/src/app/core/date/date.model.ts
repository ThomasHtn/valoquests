/**
 * Time left until the end of a weekly period, in whole units.
 */
export interface RemainingTime {
  /**
   * Whole days left.
   */
  readonly days: number;

  /**
   * Hours left past the whole days.
   */
  readonly hours: number;

  /**
   * Minutes left past the whole hours.
   */
  readonly minutes: number;
}

/**
 * Numeric fields of a `YYYY-MM-DD` date.
 */
export interface IsoDateParts {
  /**
   * Full year.
   */
  readonly year: number;

  /**
   * Month, 1 for January.
   */
  readonly month: number;

  /**
   * Day of the month.
   */
  readonly day: number;
}
