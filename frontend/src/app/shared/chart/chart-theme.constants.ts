/**
 * Number of series the validated palette covers.
 *
 * A caller with more entities than this folds the extras away rather than generating a sixth hue:
 * see {@link SERIES_COLOR_VARIABLES}.
 */
export const SERIES_COLOR_COUNT = 5;

/**
 * Theme variables holding the chart series palette, in the order they must be assigned.
 *
 * The order is not cosmetic. The palette was validated as an ordered set against the dark page
 * surface — lightness band, chroma floor, colorblind separation between *adjacent* slots, contrast
 * — and permuting it drops the worst deuteranopia pair from ΔE 9.9 to 3.9, which is two curves a
 * colorblind reader cannot tell apart. Assign slots in sequence and re-run the data-viz validator
 * before touching either this list or the values behind it in `styles/colors.css`.
 */
export const SERIES_COLOR_VARIABLES = [
  '--color-series-1',
  '--color-series-2',
  '--color-series-3',
  '--color-series-4',
  '--color-series-5',
] as const;

/**
 * Font of an axis title. Larger than a tick label, since it names the whole axis rather than one
 * value on it.
 */
export const AXIS_TITLE_FONT = {
  family: 'Barlow Condensed, sans-serif',
  size: 13,
  weight: 600,
} as const;

/**
 * Font of an axis tick label.
 */
export const AXIS_TICK_FONT = {
  family: 'Barlow Condensed, sans-serif',
  size: 13,
} as const;
