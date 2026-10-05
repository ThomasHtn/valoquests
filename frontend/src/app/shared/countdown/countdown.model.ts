/**
 * Time unit of a countdown slot, also its translation key under `common.countdown.units`.
 */
export type CountdownUnitKey = 'days' | 'hours' | 'minutes' | 'seconds';

/**
 * One slot of a countdown: a figure and its unit.
 */
export interface CountdownUnit {
  /**
   * Figure, zero-padded except for the days.
   */
  readonly value: string;

  /**
   * Unit, printed as its translated letter after the figure.
   */
  readonly key: CountdownUnitKey;
}
