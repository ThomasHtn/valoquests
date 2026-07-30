/**
 * Relative widths, as percentages, of the placeholder lines rendered in a loading skeleton.
 *
 * Shared by every skeleton so they all show the same number of placeholder rows, with uneven
 * widths rather than identical bars, which reads as text rather than as a progress indicator. The
 * count matches the typical page size of the screens it stands in for.
 */
export const SKELETON_ROWS: readonly number[] = [62, 45, 55, 40, 58];
