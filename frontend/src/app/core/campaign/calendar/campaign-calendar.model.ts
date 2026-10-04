/**
 * Wall-clock fields of an instant, as read in the campaign time zone.
 */
export interface WallClock {
  /**
   * Four-digit year.
   */
  readonly year: number;

  /**
   * Month, one-based.
   */
  readonly month: number;

  /**
   * Day of the month.
   */
  readonly day: number;

  /**
   * Hour, 0 to 23.
   */
  readonly hour: number;

  /**
   * Minute, 0 to 59.
   */
  readonly minute: number;

  /**
   * Second, 0 to 59.
   */
  readonly second: number;
}
