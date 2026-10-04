/**
 * Series the validated palette covers; extras are folded away, never given a new hue.
 */
export const SERIES_COLOR_COUNT = 5;

/**
 * Series palette variables, assigned in this order: it was validated for colorblind separation of
 * adjacent slots, so re-run the data-viz validator before changing it or `styles/colors.css`.
 */
export const SERIES_COLOR_VARIABLES = [
  '--color-series-1',
  '--color-series-2',
  '--color-series-3',
  '--color-series-4',
  '--color-series-5',
] as const;

/**
 * Font of an axis title.
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
