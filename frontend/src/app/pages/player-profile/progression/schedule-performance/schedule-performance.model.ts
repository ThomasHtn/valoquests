import { ChartBar } from '@shared/chart/chart.model';

/**
 * One of the two win-rate charts, by weekday or by time of day.
 */
export interface ScheduleChart {
  /**
   * Translation key of the chart's caption, also its accessible name.
   */
  readonly titleKey: string;

  /**
   * Translation key of the horizontal axis.
   */
  readonly xAxisKey: string;

  /**
   * Bars in display order.
   */
  readonly bars: readonly ChartBar[];

  /**
   * The chart as text for screen readers.
   */
  readonly summary: string;

  /**
   * Translated sentence naming the best slot, empty when none qualifies.
   */
  readonly best: string;
}
