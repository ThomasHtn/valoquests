import { ProfileViewOption } from './player-profile.model';

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
 * Span modifier of each skeleton tile, mirroring the loaded stat tiles.
 */
export const STAT_SKELETON_TILE_MODIFIERS: readonly string[] = [
  'player-profile__stat--lead',
  '',
  '',
  '',
  '',
  'player-profile__stat--trail',
];

/**
 * Buttons of the display switch, in display order.
 */
export const PROFILE_VIEWS: readonly ProfileViewOption[] = [
  { mode: 'MATCHES', labelKey: 'playerProfile.display.matches' },
  { mode: 'PROGRESS', labelKey: 'playerProfile.display.progress' },
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
