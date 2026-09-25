/**
 * Number of game modes given their own button after "all modes", by minimum width of the filter
 * bar, widest first. The remaining `FILTERABLE_GAME_MODES` fall into the overflow menu.
 *
 * Measured so the season filter, the display switch and the reset button still fit beside the
 * group without scrolling the bar. Capped below the full list so the overflow menu never ends up
 * empty.
 */
export const GAME_MODE_BUTTON_COUNTS: readonly { minRowWidthPx: number; count: number }[] = [
  { minRowWidthPx: 1050, count: 6 },
  { minRowWidthPx: 985, count: 5 },
  { minRowWidthPx: 900, count: 4 },
  { minRowWidthPx: 765, count: 3 },
  { minRowWidthPx: 0, count: 2 },
];

/**
 * Largest number of seasons the progression view will chart at once.
 *
 * The chart series palette holds exactly this many slots, validated as an ordered set against the
 * page's surface; a sixth curve would have to be a generated hue, which is how a chart ends up
 * with two colours a colourblind reader cannot separate. See `styles/colors.css`.
 */
export const MAX_PROGRESSION_SEASONS = 5;

/**
 * Shared column grid for every row of the desktop match-history grid (header, day-summary and
 * match rows alike), so their columns land at the same horizontal position however each row is
 * otherwise styled. A CSS Grid rather than an HTML `<table>`: match rows need a real inset margin
 * to read as nested under the day-summary row above them, and `margin` has no effect on `<tr>`.
 *
 * The 8 stat columns are `fr`-based, not fixed widths: a fixed width keeps them pinned to their
 * own narrow band regardless of how wide the row grows.
 */
export const MATCH_ROW_GRID_CLASS =
  'grid grid-cols-[minmax(0,2fr)_repeat(8,minmax(0,1fr))] items-center';
