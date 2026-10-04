/**
 * Game mode buttons after "all modes", by minimum filter bar width; the rest go to the overflow.
 * Sized so the other controls fit unscrolled; below the full list so the overflow is never empty.
 */
export const GAME_MODE_BUTTON_COUNTS: readonly { minRowWidthPx: number; count: number }[] = [
  { minRowWidthPx: 1050, count: 6 },
  { minRowWidthPx: 985, count: 5 },
  { minRowWidthPx: 900, count: 4 },
  { minRowWidthPx: 765, count: 3 },
  { minRowWidthPx: 0, count: 2 },
];

/**
 * Most seasons charted at once: the colourblind-safe series palette has exactly five slots.
 */
export const MAX_PROGRESSION_SEASONS = 5;

/**
 * Stat strip grid, shared with its skeleton; the win rate leads with the wide column.
 */
export const STAT_STRIP_GRID_CLASS =
  'grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-[minmax(0,1.6fr)_repeat(5,minmax(0,1fr))]';

/**
 * Skeleton tile spans, mirroring the loaded stat tiles.
 */
export const STAT_SKELETON_TILE_SPANS: readonly string[] = [
  'col-span-2 sm:col-span-3 lg:col-span-1',
  '',
  '',
  '',
  '',
  'col-span-2 lg:col-span-1',
];

/**
 * Query parameters holding the profile state.
 */
export const PROFILE_QUERY_KEYS = { view: 'view', mode: 'mode', season: 'season' } as const;

/**
 * `view` value of the progression view; the history is the default.
 */
export const PROGRESS_VIEW_PARAM = 'progress';

/**
 * `season` value for every season; no parameter means the current one.
 */
export const ALL_SEASONS_PARAM = 'all';
