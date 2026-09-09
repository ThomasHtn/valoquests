import { GameMode } from '@core/matches/game-mode.model';

/**
 * Game mode the profile opens on. Statistics are always scoped to one concrete mode - an "all
 * modes" aggregate would mix incomparable queues - so there is no nullable default.
 */
export const DEFAULT_GAME_MODE: GameMode = 'COMPETITIVE';

/**
 * Game modes shown as their own button in the game-mode filter's button group, in
 * `FILTERABLE_GAME_MODES` order. The remaining modes stay reachable through that group's overflow
 * menu rather than crowding the group itself.
 */
export const PRIMARY_GAME_MODES: readonly GameMode[] = ['COMPETITIVE', 'UNRATED', 'DEATHMATCH'];

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
