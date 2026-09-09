/**
 * One row of the chart's legend: a season, its color, and the average it held over that season.
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
   * Season average, formatted.
   */
  readonly average: string;
}
