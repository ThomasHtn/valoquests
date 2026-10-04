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
