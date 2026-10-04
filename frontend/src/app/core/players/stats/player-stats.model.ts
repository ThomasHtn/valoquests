/**
 * Text and bar colours of a statistic; full literal classes so Tailwind's scanner finds them.
 */
export interface StatVisual {
  /**
   * Tailwind text colour class.
   */
  readonly textClass: string;

  /**
   * Tailwind background class of the bar.
   */
  readonly barClass: string;
}
