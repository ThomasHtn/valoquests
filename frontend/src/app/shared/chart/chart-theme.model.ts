/**
 * Colors every chart borrows from the design system, resolved once per chart build.
 */
export interface ChartTheme {
  /**
   * Grid lines and axis borders, recessive enough to sit behind the data.
   */
  readonly grid: string;

  /**
   * Axis tick labels.
   */
  readonly tick: string;

  /**
   * Tooltip background.
   */
  readonly tooltipSurface: string;

  /**
   * Tooltip border.
   */
  readonly tooltipBorder: string;

  /**
   * Tooltip text.
   */
  readonly tooltipText: string;

  /**
   * The state color marking a player's strongest slot on the schedule charts.
   */
  readonly highlight: string;

  /**
   * Fill of a bar whose sample is too small to be judged.
   */
  readonly muted: string;
}
