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
 * Visual treatment for one timeline node status: its hex marker's frame color/glow, status badge,
 * and the connector line leading to the next node.
 *
 * Flat and glow-free for resolved outcomes (the frame color alone carries the meaning), reserving
 * the pulsing glow for the one node that represents the fight actually in progress.
 */
export interface BossTimelineTier {
  /**
   * Stroke color utility applied to the hex marker's segmented frame.
   */
  readonly frameStrokeClass: string;

  /**
   * Glow/motion utility layered on the hex marker, empty for statuses with no special emphasis.
   */
  readonly frameGlowClass: string;

  /**
   * Background/text color utilities applied to the node's status badge.
   */
  readonly badgeClass: string;

  /**
   * Background utility applied to the connector line segment leading to the next node.
   */
  readonly connectorClass: string;
}

/**
 * Tier treatment indexed by {@link BossTimelineNodeStatus}.
 */
const BOSS_TIMELINE_TIERS: Readonly<Record<BossTimelineNodeStatus, BossTimelineTier>> = {
  defeated: {
    frameStrokeClass: 'stroke-accent-green',
    frameGlowClass: '',
    badgeClass: 'bg-accent-green/15 text-accent-green',
    connectorClass: 'bg-accent-green/40',
  },
  survived: {
    frameStrokeClass: 'stroke-text-muted',
    frameGlowClass: '',
    badgeClass: 'bg-surface-700 text-text-muted',
    connectorClass: 'bg-surface-700',
  },
  current: {
    frameStrokeClass: 'stroke-accent-purple',
    frameGlowClass: 'drop-shadow-[0_0_10px_var(--color-accent-purple)] motion-safe:animate-pulse',
    badgeClass: 'bg-accent-purple/15 text-accent-purple',
    connectorClass: 'bg-surface-700',
  },
  upcoming: {
    frameStrokeClass: 'stroke-surface-600',
    frameGlowClass: 'opacity-50',
    badgeClass: 'bg-surface-800 text-text-muted',
    connectorClass: 'bg-surface-800',
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
 * One edge of the hex marker's segmented frame, as a line in a 0-100 square viewBox.
 */
export interface HexFrameEdge {
  readonly x1: number;
  readonly y1: number;
  readonly x2: number;
  readonly y2: number;
}

/**
 * The six edges of the hex marker's frame, each shortened at both ends so a small gap opens at
 * every vertex — the segmented, beveled look of a sci-fi hex frame rather than a plain outline.
 *
 * Traces the same pointy-top hexagon already used to clip podium avatars on the ranking page
 * (`clip-path:polygon(50% 0%,100% 25%,100% 75%,50% 100%,0% 75%,0% 25%)`), so the boss portrait and
 * its frame share one consistent hex geometry across the app.
 */
export const HEX_FRAME_EDGES: readonly HexFrameEdge[] = [
  { x1: 57.5, y1: 3.75, x2: 92.5, y2: 21.25 },
  { x1: 100, y1: 32.5, x2: 100, y2: 67.5 },
  { x1: 92.5, y1: 78.75, x2: 57.5, y2: 96.25 },
  { x1: 42.5, y1: 96.25, x2: 7.5, y2: 78.75 },
  { x1: 0, y1: 67.5, x2: 0, y2: 32.5 },
  { x1: 7.5, y1: 21.25, x2: 42.5, y2: 3.75 },
];
