/**
 * Quadratic ease-in-out of a 0 to 1 progress, the gauges' curve, so a figure and its bar arrive together.
 */
export function easeInOutQuad(progress: number): number {
  return progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(2 - 2 * progress, 2) / 2;
}
