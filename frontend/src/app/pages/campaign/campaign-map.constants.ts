import { BossTimelineNodeStatus } from '@core/boss/boss-timeline.constants';

/**
 * Columns of the hex grid the campaign is laid over, as the list the template iterates.
 *
 * Thirteen wide, but only the five middle ones are ever guaranteed on screen: the outer columns
 * are pure terrain that widens the field as the viewport allows (see
 * {@link resolveColumnVisibilityClass}), so the map fills the page out to its edges on a desktop
 * without ever pushing the path off a phone.
 */
export const MAP_COLUMNS: readonly number[] = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

/**
 * Column the week at each position of the campaign occupies, before {@link PATH_COLUMN_OFFSET} is
 * applied.
 *
 * A ping-pong across five columns: consecutive weeks never sit more than one column apart, so the
 * ground taken reads as one contiguous front snaking down the map rather than as cells scattered
 * over it. The period is deliberately not a multiple of that width — the path reverses at both
 * edges instead of wrapping around, which is what makes it serpentine.
 */
const BOSS_COLUMN_PATTERN: readonly number[] = [2, 1, 0, 1, 2, 3, 4, 3];

/**
 * Shift applied to {@link BOSS_COLUMN_PATTERN} so the path runs down the five middle columns,
 * leaving four columns of terrain on either side.
 */
const PATH_COLUMN_OFFSET = 4;

/**
 * Number of terrain-only rows drawn before the first week and after the last.
 *
 * The campaign is a front moving through a territory, not a territory that starts and ends with it,
 * so the field keeps going past both ends of the path. They also give a short campaign enough body
 * to fill the fold. Kept even at the head so the odd/even row offset of the weeks below is
 * unchanged by their presence.
 */
export const LEAD_TERRAIN_ROWS = 2;
export const TRAIL_TERRAIN_ROWS = 2;

/**
 * Fraction the inner hexagon is scaled down to, which is what draws a tile's outline.
 *
 * `clip-path` drops an element's borders, so the outline has to be a second hexagon showing through
 * from behind the surface. Inset from the edges it would not be even: an inset moves every edge by
 * the same *axis-aligned* distance, which on a hexagon's slanted edges is a shorter perpendicular
 * one, so the outline came out thinner at the points than along the sides. Scaling instead is a
 * homothety, and `clip-hex` on a `1 : 1.1547` box is a regular hexagon, so every edge moves inward
 * by the same perpendicular distance — a border of constant width, whatever the tile's size.
 */
export const TILE_INNER_SCALE = 0.95;

/**
 * Same construction as {@link TILE_INNER_SCALE}, for the legend's much smaller sample hexagons.
 * They need a proportionally thicker outline than a tile to read as the same shape at that size.
 */
export const LEGEND_INNER_SCALE = 0.84;

/**
 * Visual treatment of one territory hexagon, split across the two layers it is drawn with — see
 * {@link TILE_INNER_SCALE} for how the outline is drawn.
 */
export interface BossTerritoryTier {
  /**
   * Background of the outer hexagon, read as the territory's outline.
   */
  readonly ringClass: string;

  /**
   * Background of the inner hexagon, read as the ground itself.
   */
  readonly fillClass: string;

  /**
   * Color of the outcome icon carved into the hexagon.
   */
  readonly iconClass: string;

  /**
   * Utilities for the halo hexagon layered behind the tile, empty for every status but the week
   * currently being fought — the only one allowed to pulse.
   */
  readonly haloClass: string;

  /**
   * Background of the layer rising inside the hexagon in proportion to the damage dealt, so a
   * territory visibly fills up as the group takes it.
   */
  readonly damageFillClass: string;
}

/**
 * Territory treatment indexed by week status.
 *
 * The four states are told through color alone, in the same vocabulary the rest of the campaign
 * uses: brand amber for ground taken, red for the fight running right now, a dulled red for an
 * assault that was repelled, and bare surface for ground nobody has reached yet.
 */
const BOSS_TERRITORY_TIERS: Readonly<Record<BossTimelineNodeStatus, BossTerritoryTier>> = {
  defeated: {
    ringClass: 'bg-brand-500',
    fillClass: 'bg-brand-500/18',
    iconClass: 'text-brand-400',
    haloClass: '',
    damageFillClass: 'bg-brand-500/35',
  },
  survived: {
    ringClass: 'bg-accent-red/45',
    fillClass: 'bg-surface-800',
    iconClass: 'text-accent-red/80',
    haloClass: '',
    damageFillClass: 'bg-accent-red/15',
  },
  current: {
    ringClass: 'bg-accent-red',
    fillClass: 'bg-surface-800',
    iconClass: 'text-accent-red',
    haloClass: 'bg-accent-red/25 motion-safe:animate-pulse',
    damageFillClass: 'bg-accent-red/35',
  },
  upcoming: {
    ringClass: 'bg-surface-600',
    fillClass: 'bg-surface-800',
    iconClass: 'text-text-muted',
    haloClass: '',
    damageFillClass: '',
  },
};

/**
 * Treatment of the neutral ground between and around the weeks.
 *
 * One flat treatment for the whole field, deliberately: the weeks are what carry the campaign's
 * state, and tinting the terrain by how far the front had passed only read as an unexplained color
 * change across the top of the map.
 */
export const TERRAIN_RING_CLASS = 'bg-surface-800/50';
export const TERRAIN_FILL_CLASS = 'bg-surface-950';

/**
 * Viewport width each column of {@link MAP_COLUMNS} needs before it is drawn, as the utilities that
 * reveal it. The five middle columns — the ones the path runs through — are always present; each
 * ring further out needs a wider viewport, so the field grows toward the component's edges as the
 * breakpoints climb rather than jumping straight to full width.
 */
const COLUMN_VISIBILITY_CLASSES: Readonly<Record<number, string>> = {
  0: 'hidden 2xl:block',
  1: 'hidden xl:block',
  2: 'hidden lg:block',
  3: 'hidden sm:block',
  9: 'hidden sm:block',
  10: 'hidden lg:block',
  11: 'hidden xl:block',
  12: 'hidden 2xl:block',
};

/**
 * Statuses listed in the map's legend, in the order the campaign runs through them.
 *
 * Four colors over otherwise identical shapes are not self-explanatory, so the map states what each
 * one means rather than leaving it to be inferred.
 */
export const MAP_LEGEND_STATUSES: readonly BossTimelineNodeStatus[] = [
  'defeated',
  'current',
  'survived',
  'upcoming',
];

/**
 * Resolves the column a week occupies on the map.
 *
 * @param index - Position of the week in the campaign, oldest first.
 * @returns The zero-based column index, within {@link MAP_COLUMNS}.
 */
export function resolveBossColumn(index: number): number {
  return BOSS_COLUMN_PATTERN[index % BOSS_COLUMN_PATTERN.length] + PATH_COLUMN_OFFSET;
}

/**
 * Resolves the utilities gating a column on viewport width.
 *
 * @param column - Zero-based column index, within {@link MAP_COLUMNS}.
 * @returns The Tailwind display utilities, or the empty string for a column always drawn.
 */
export function resolveColumnVisibilityClass(column: number): string {
  return COLUMN_VISIBILITY_CLASSES[column] ?? '';
}

/**
 * Resolves the visual treatment of a week's territory.
 *
 * @param status - The week's outcome/state.
 * @returns The tier to render the hexagon with.
 */
export function resolveBossTerritoryTier(status: BossTimelineNodeStatus): BossTerritoryTier {
  return BOSS_TERRITORY_TIERS[status];
}
