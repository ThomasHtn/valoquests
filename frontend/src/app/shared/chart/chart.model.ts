/**
 * One plotted curve.
 */
export interface ChartSeries {
  /**
   * Translated name of the entity the curve stands for.
   */
  readonly label: string;

  /**
   * Curve color, from the series palette by the entity's stable rank.
   */
  readonly color: string;

  /**
   * Values per axis position; leading `null`s right-align a shorter series.
   */
  readonly points: readonly (number | null)[];

  /**
   * Shades this curve's area, overriding the chart's `filled`.
   */
  readonly filled?: boolean;

  /**
   * Dashed, so two neighbors of close color stay apart.
   */
  readonly dashed?: boolean;
}

/**
 * One bar of a categorical bar chart.
 */
export interface ChartBar {
  /**
   * Translated category name, shown on the axis.
   */
  readonly label: string;

  /**
   * The plotted value.
   */
  readonly value: number;

  /**
   * Formatted value printed above the bar, defaults to the chart's value formatter.
   */
  readonly valueLabel?: string;

  /**
   * Translated tooltip second line, typically the sample size.
   */
  readonly detail: string;

  /**
   * Highlighted bar, drawn in the "good" state color.
   */
  readonly highlighted: boolean;

  /**
   * Sample too small to judge: drawn recessive so it is not misread as a result.
   */
  readonly muted: boolean;
}

/**
 * Where the mark under the pointer sits, for a tooltip drawn in HTML beside the canvas.
 */
export interface ChartTooltipAnchor {
  /**
   * Index of the hovered data point in the first dataset.
   */
  readonly index: number;

  /**
   * Mark x, in CSS pixels from the canvas' left edge.
   */
  readonly x: number;

  /**
   * Mark y, in CSS pixels from the canvas' top edge.
   */
  readonly y: number;

  /**
   * Width of the canvas, in CSS pixels.
   */
  readonly chartWidth: number;

  /**
   * Height of the canvas, in CSS pixels.
   */
  readonly chartHeight: number;
}

/**
 * Resolved position of an HTML chart tooltip inside the canvas' box.
 */
export interface ChartTooltipPlacement {
  /**
   * Left offset of the anchor point, in CSS pixels.
   */
  readonly left: number;

  /**
   * Top offset of the anchor point, in CSS pixels.
   */
  readonly top: number;

  /**
   * CSS transform moving the bubble off the mark.
   */
  readonly transform: string;
}

/**
 * Writes a plotted value for a tooltip, unit included (`23,4 %`).
 */
export type ChartValueFormatter = (value: number) => string;
