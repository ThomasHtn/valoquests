/**
 * Legend row: a season, its colour and its season average.
 */
export interface EvolutionLegendEntry {
  /**
   * Season name.
   */
  readonly label: string;

  /**
   * CSS colour of the series.
   */
  readonly color: string;

  /**
   * Formatted season average.
   */
  readonly average: string;
}

/**
 * Plottable metric; swapped rather than stacked since their units differ (no second y axis).
 */
export type EvolutionMetric = 'headshotPercentage' | 'kda' | 'acs' | 'adr';
