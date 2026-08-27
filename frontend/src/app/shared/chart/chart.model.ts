/**
 * One plotted curve.
 */
export interface ChartSeries {
  /**
   * Already-translated name of the entity the curve stands for.
   */
  readonly label: string;

  /**
   * The curve's color, taken from the validated series palette by the entity's own stable rank.
   */
  readonly color: string;

  /**
   * The curve's values, one per position on the shared axis.
   *
   * Leading `null`s are how a shorter series is pushed to the right so every curve ends on the
   * same abscissa; Chart.js draws nothing for them.
   */
  readonly points: readonly (number | null)[];
}

/**
 * One arc of a half-donut gauge.
 */
export interface ChartGaugeSegment {
  /**
   * The arc's own weight, out of the sum of every segment's weight. Equal across every segment for
   * a gauge whose slots must all read the same size regardless of what they hold (the food ring's
   * days); a fill/track pair for a gauge reading a single percentage instead (attractivity).
   */
  readonly value: number;

  /**
   * The arc's fill.
   */
  readonly color: string;

  /**
   * Already-translated label, shown in the arc's own tooltip.
   */
  readonly label: string;
}

/**
 * One bar of a categorical bar chart.
 */
export interface ChartBar {
  /**
   * Already-translated category name, shown on the axis.
   */
  readonly label: string;

  /**
   * The plotted value.
   */
  readonly value: number;

  /**
   * Already-translated secondary line for the tooltip, typically the sample the value rests on.
   */
  readonly detail: string;

  /**
   * Whether this bar is the highlighted one, drawn in the "good" state color.
   */
  readonly highlighted: boolean;

  /**
   * Whether the bar's sample is too small to be judged, drawn recessive so it cannot be misread as
   * a result.
   */
  readonly muted: boolean;
}
