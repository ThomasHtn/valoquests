/**
 * How the fight ends: already won, won in time at the current pace, or not in time.
 */
export type FallOutcome = 'down' | 'ahead' | 'short';

/**
 * What a known reading of the guardian's hit points stands for.
 */
export type FallReadingKind = 'start' | 'dayEnd' | 'now' | 'kill';

/**
 * One known reading of the guardian's hit points.
 */
export interface FallReading {
  /**
   * What the reading stands for.
   */
  readonly kind: FallReadingKind;

  /**
   * Instant of the reading, in epoch milliseconds.
   */
  readonly time: number;

  /**
   * Hit points the guardian had left at that instant.
   */
  readonly left: number;
}

/**
 * The guardian's descent over the week, and where the current pace takes it.
 */
export interface GuardianFall {
  /**
   * How the fight ends.
   */
  readonly outcome: FallOutcome;

  /**
   * Hit points the guardian opened the week with.
   */
  readonly hitPoints: number;

  /**
   * Monday 00:00 Paris time, in epoch milliseconds.
   */
  readonly weekStart: number;

  /**
   * Sunday midnight Paris time (the extraction), in epoch milliseconds.
   */
  readonly deadline: number;

  /**
   * Known readings, oldest first: Monday's pool, each day's close, then now or the fatal blow.
   */
  readonly readings: readonly FallReading[];

  /**
   * Hit points the squad takes per millisecond, averaged since Monday.
   */
  readonly pace: number;

  /**
   * Fall instant, real or estimated, `null` when the pace does not get there.
   */
  readonly fallAt: number | null;

  /**
   * Hit points left at Sunday midnight at the current pace.
   */
  readonly leftAtDeadline: number;
}

/**
 * One tinted column of the chart.
 */
export interface FallZone {
  /**
   * Played part, projection, short projection, or spare time after the fall.
   */
  readonly kind: 'past' | 'ahead' | 'short' | 'spare';

  /**
   * Left edge, in pixels.
   */
  readonly x: number;

  /**
   * Width, in pixels.
   */
  readonly width: number;
}

/**
 * A point of the chart, in pixels.
 */
export interface FallChartPoint {
  /**
   * Horizontal position, in pixels.
   */
  readonly x: number;

  /**
   * Vertical position, in pixels.
   */
  readonly y: number;
}

/**
 * The chart's geometry, laid out for one width.
 */
export interface FallChart {
  /**
   * Width of the drawing, in pixels.
   */
  readonly width: number;

  /**
   * Height of the plot, ticks excluded, in pixels.
   */
  readonly plotHeight: number;

  /**
   * Height of the whole drawing, ticks included, in pixels.
   */
  readonly height: number;

  /**
   * Tinted columns, left to right.
   */
  readonly zones: readonly FallZone[];

  /**
   * Played part's curve, as an SVG path.
   */
  readonly pastLine: string;

  /**
   * Area under the played part's curve, as an SVG path.
   */
  readonly pastArea: string;

  /**
   * Projection line, `null` once the guardian is down.
   */
  readonly projectionLine: string | null;

  /**
   * Area under the projection, `null` once the guardian is down.
   */
  readonly projectionArea: string | null;

  /**
   * Current reading, `null` once the guardian is down.
   */
  readonly now: FallChartPoint | null;

  /**
   * Projection at Sunday midnight when it falls short, otherwise `null`.
   */
  readonly end: FallChartPoint | null;

  /**
   * Fall point on the chart floor, `null` when it does not come in time.
   */
  readonly fall: FallChartPoint | null;

  /**
   * Text anchor keeping the fall's label inside the drawing.
   */
  readonly fallAnchor: 'start' | 'middle' | 'end';

  /**
   * Side gutter of the first and last ticks, in pixels.
   */
  readonly gutter: number;
}

/**
 * What the descent says at one instant.
 */
export interface FallPointer {
  /**
   * Known reading, projection, projection at zero, or a moment after the fall.
   */
  readonly kind: FallReadingKind | 'estimate' | 'zero' | 'after';

  /**
   * Instant in epoch milliseconds, snapped to a known reading over the played part.
   */
  readonly time: number;

  /**
   * Hit points left at that instant.
   */
  readonly left: number;
}

/**
 * What the pointer reads at one position of the chart.
 */
export interface FallHover {
  /**
   * Crosshair x, snapped to a known reading in the played part.
   */
  readonly x: number;

  /**
   * Height of the curve under the crosshair.
   */
  readonly y: number;

  /**
   * Translated time of the reading.
   */
  readonly when: string;

  /**
   * Translated value of the reading.
   */
  readonly value: string;
}

/**
 * The guardian's pool and the week's span a descent is measured against.
 */
export type FallBounds = Pick<GuardianFall, 'hitPoints' | 'weekStart' | 'deadline'>;
