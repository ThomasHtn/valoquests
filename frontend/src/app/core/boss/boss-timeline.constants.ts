/**
 * Outcome of one timeline node, driving its hex marker, badge and connector treatment.
 *
 * `'defeated'` and `'survived'` are finalized past weeks (see `BossHistoryWeek.defeated`),
 * `'current'` is the active week's ongoing confrontation, and `'upcoming'` is a placeholder for a
 * future week whose boss hasn't been drawn yet (the backend only ever draws a week's boss lazily,
 * once that week becomes current — see `DefaultWeeklyBossSelectionService` — so no real data exists
 * for weeks ahead).
 */
export type BossTimelineNodeStatus = 'defeated' | 'survived' | 'current' | 'upcoming';

/**
 * Visual treatment for one timeline node status: its hex week marker, the panel silhouette beside
 * it, and the damage bar inside that panel.
 *
 * The four statuses form a single reading of the campaign, told through color alone: brand amber
 * for a boss the group put down, red for the fight currently running, muted surfaces for a week
 * that was merely survived, and a dimmer step still for a week whose boss has not been drawn.
 * Flat and glow-free everywhere except the active week, which is the only node allowed to pulse.
 */
export interface BossTimelineTier {
  /**
   * Background (hex ring) and text (week number) color utilities applied to the marker.
   */
  readonly markerClass: string;

  /**
   * Utilities for the halo hexagon layered behind the marker, empty for every status but the
   * active week.
   */
  readonly markerHaloClass: string;

  /**
   * Border and gradient-start utilities tinting the node's panel with the status color.
   */
  readonly panelClass: string;

  /**
   * Background/text color utilities applied to the node's status pill.
   */
  readonly pillClass: string;

  /**
   * Background utility applied to the health bar's fill.
   */
  readonly barFillClass: string;

  /**
   * Text color utility for the values the status color owns: the remaining hit points percentage
   * and the bullet ahead of the node's meta line.
   */
  readonly accentTextClass: string;
}

/**
 * Tier treatment indexed by {@link BossTimelineNodeStatus}.
 */
const BOSS_TIMELINE_TIERS: Readonly<Record<BossTimelineNodeStatus, BossTimelineTier>> = {
  defeated: {
    markerClass: 'bg-brand-500 text-brand-500',
    markerHaloClass: '',
    panelClass: 'border-brand-500/40 from-brand-500/12',
    pillClass: 'bg-brand-500/15 text-brand-500',
    barFillClass: 'bg-brand-500',
    accentTextClass: 'text-brand-500',
  },
  survived: {
    markerClass: 'bg-surface-600 text-text-secondary',
    markerHaloClass: '',
    panelClass: 'border-surface-700 from-surface-700/35',
    pillClass: 'bg-surface-800 text-text-secondary',
    barFillClass: 'bg-surface-600',
    accentTextClass: 'text-text-secondary',
  },
  current: {
    markerClass: 'bg-accent-red text-accent-red',
    markerHaloClass: 'bg-accent-red/25 motion-safe:animate-pulse',
    panelClass: 'border-accent-red/50 from-accent-red/14',
    pillClass: 'bg-accent-red/15 text-accent-red',
    barFillClass: 'bg-accent-red',
    accentTextClass: 'text-accent-red',
  },
  upcoming: {
    markerClass: 'bg-surface-700 text-text-muted',
    markerHaloClass: '',
    panelClass: 'border-surface-800 from-surface-800/25',
    pillClass: 'bg-surface-800 text-text-muted',
    barFillClass: 'bg-surface-700',
    accentTextClass: 'text-text-muted',
  },
};

/**
 * Resolves the visual tier for a timeline node's status.
 *
 * @param status - The node's outcome/state.
 * @returns The tier treatment to render.
 */
export function resolveBossTimelineTier(status: BossTimelineNodeStatus): BossTimelineTier {
  return BOSS_TIMELINE_TIERS[status];
}

/**
 * Translation key for a timeline node's status badge, indexed by {@link BossTimelineNodeStatus}.
 *
 * A finalized week's badge reuses the existing `boss.outcome.*` keys; the active and locked states
 * get their own `boss.status.*` keys since neither is an "outcome".
 */
const BOSS_TIMELINE_STATUS_LABEL_KEYS: Readonly<Record<BossTimelineNodeStatus, string>> = {
  defeated: 'boss.outcome.defeated',
  survived: 'boss.outcome.survived',
  current: 'boss.status.current',
  upcoming: 'boss.status.upcoming',
};

/**
 * Resolves the translation key for a timeline node's status badge.
 *
 * @param status - The node's outcome/state.
 * @returns The i18n key to resolve through {@link TranslatePipe}.
 */
export function resolveBossStatusLabelKey(status: BossTimelineNodeStatus): string {
  return BOSS_TIMELINE_STATUS_LABEL_KEYS[status];
}

/**
 * Translation key captioning a node's health bar, indexed by {@link BossTimelineNodeStatus}.
 *
 * The same bar means three different things depending on the week it belongs to — the hit points
 * left to take off, the empty gauge of a boss put down, or what a week fell short of — so each
 * status names it rather than sharing one neutral caption.
 *
 * `'upcoming'` maps to the empty string: a locked week renders no bar at all.
 */
const BOSS_TIMELINE_BAR_LABEL_KEYS: Readonly<Record<BossTimelineNodeStatus, string>> = {
  defeated: 'boss.hpBar.defeated',
  survived: 'boss.hpBar.survived',
  current: 'boss.hpBar.current',
  upcoming: '',
};

/**
 * Resolves the translation key captioning a timeline node's health bar.
 *
 * @param status - The node's outcome/state.
 * @returns The i18n key to resolve, or the empty string for a status rendering no bar.
 */
export function resolveBossHpBarLabelKey(status: BossTimelineNodeStatus): string {
  return BOSS_TIMELINE_BAR_LABEL_KEYS[status];
}

/**
 * Visual treatment of one territory hexagon on the campaign card, split across the two layers it
 * is drawn with — see `TILE_INNER_SCALE` for how the outline is drawn, and `TERRITORY_FILL_CLASS`
 * for the ground they share.
 */
export interface BossTerritoryTier {
  /**
   * Background of the outer hexagon, read as the territory's outline — and, the tiles being
   * hollow, the whole of what the state is told with. Doubles as the state's own fill wherever a
   * solid surface says the same thing, the detail panel's health bar being the one such place.
   */
  readonly ringClass: string;

  /**
   * Border colour of the seam joining the detail panel to the map above it, so the panel visibly
   * comes out of the hexagon that was clicked rather than merely appearing under the card.
   */
  readonly seamClass: string;

  /**
   * Color of the outcome icon carved into the hexagon.
   */
  readonly iconClass: string;

  /**
   * Utilities for the halo hexagon layered behind the tile, empty for every status but the week
   * currently being fought.
   */
  readonly haloClass: string;

  /**
   * Background of the layer rising inside the hexagon in proportion to the damage dealt, so a
   * territory visibly fills up as the group takes it.
   */
  readonly damageFillClass: string;

  /**
   * Text colour tinting the shockwave expanding out of the tile, and the crest riding on its fill
   * — both take theirs from `currentColor`. Empty on every status but the week being fought, which
   * is the only tile whose numbers are still moving.
   */
  readonly liveAccentClass: string;
}

/**
 * Territory treatment indexed by week status.
 *
 * An outcome vocabulary, not a chronological one: green for ground taken, red for a week the boss
 * held, the direction's amber for the fight running right now, bare surface for ground nobody has
 * reached yet.
 */
const BOSS_TERRITORY_TIERS: Readonly<Record<BossTimelineNodeStatus, BossTerritoryTier>> = {
  defeated: {
    ringClass: 'bg-success',
    seamClass: 'border-t-success',
    iconClass: 'text-success',
    haloClass: '',
    damageFillClass: 'bg-brand-500/28',
    liveAccentClass: '',
  },
  survived: {
    ringClass: 'bg-danger',
    seamClass: 'border-t-danger',
    iconClass: 'text-danger',
    haloClass: '',
    damageFillClass: 'bg-text-primary/10',
    liveAccentClass: '',
  },
  current: {
    ringClass: 'bg-brand-500',
    seamClass: 'border-t-brand-500',
    iconClass: 'text-brand-500',
    haloClass: 'bg-brand-500/16',
    damageFillClass: 'bg-brand-500/28',
    liveAccentClass: 'text-brand-500/40',
  },
  upcoming: {
    ringClass: 'bg-surface-600',
    seamClass: 'border-t-surface-600',
    iconClass: 'text-text-muted',
    haloClass: '',
    damageFillClass: '',
    liveAccentClass: '',
  },
};

/**
 * Resolves the visual treatment of a week's territory.
 *
 * @param status - The week's outcome/state.
 * @returns The tier to render the hexagon with.
 */
export function resolveBossTerritoryTier(status: BossTimelineNodeStatus): BossTerritoryTier {
  return BOSS_TERRITORY_TIERS[status];
}

/**
 * Fraction the inner hexagon is scaled down to, which is what draws a tile's outline — see
 * {@link BossTerritoryTier.ringClass}. Scaling rather than insetting keeps a constant border width
 * all the way around a `clip-hex` shape, whatever the tile's size.
 */
export const TILE_INNER_SCALE = 0.95;

/**
 * Same construction as {@link TILE_INNER_SCALE}, for the legend's much smaller sample hexagons.
 */
export const LEGEND_INNER_SCALE = 0.84;

/**
 * Ground inside every territory hexagon, tiles and legend samples alike — a territory is read by
 * its outline, not by a tint of the state's colour.
 */
export const TERRITORY_FILL_CLASS = 'bg-surface-950';

/**
 * Treatment of the neutral ground framing the row of territories at each end — a veil over the
 * page's own ground, drawn by the `hex-terrain` utility.
 */
export const TERRAIN_RING_CLASS = 'bg-text-primary/8';
