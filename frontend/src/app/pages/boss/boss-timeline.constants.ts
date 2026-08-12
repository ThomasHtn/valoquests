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
