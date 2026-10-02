import { HULL_FIGURE_MIN_SIZE, HULL_FIGURE_SIZES } from './extraction-gauges.constants';

/**
 * The font size that keeps the aboard figure inside the rocket's hull.
 *
 * @param value - The figure shown, wounded aboard.
 * @returns A CSS length for the figure's `--fig`.
 */
export function hullFigureSize(value: number): string {
  const digits = String(Math.max(0, Math.round(value))).length;
  return HULL_FIGURE_SIZES.find((step) => digits <= step.maxDigits)?.size ?? HULL_FIGURE_MIN_SIZE;
}
