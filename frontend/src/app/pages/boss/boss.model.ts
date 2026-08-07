import { BossTimelineNodeStatus } from './boss-timeline.constants';

/**
 * One marker on the boss battle timeline, display-ready.
 *
 * Covers all four states the timeline renders: a finalized past week (`'defeated'` /
 * `'survived'`), the active week (`'current'`), or a locked placeholder for a week ahead
 * (`'upcoming'`) whose boss doesn't exist yet — see {@link BossTimelineNodeStatus}.
 */
export interface BossTimelineNode {
  /**
   * Stable identity for the `@for` track expression: a real node's ISO `weekStart`, or a
   * synthetic key for an `'upcoming'` placeholder.
   */
  readonly id: string;

  readonly status: BossTimelineNodeStatus;

  /**
   * `null` for an `'upcoming'` placeholder, whose week isn't determined yet.
   */
  readonly weekLabel: string | null;
  readonly dateRangeLabel: string | null;

  readonly bossName: string;
  readonly bossDescription: string;

  /**
   * `null` for an `'upcoming'` placeholder, whose boss category isn't drawn yet.
   */
  readonly categoryLabel: string | null;
  readonly categoryColorClass: string | null;
  readonly portraitUrl: string | null;

  /**
   * `null` for an `'upcoming'` placeholder, which has no hit points to track yet.
   */
  readonly effectiveHp: number | null;
  readonly totalDamageDealt: number | null;

  readonly defeatedByPlayerDisplayName: string | null;
  readonly defeatedByAvatarUrl: string | null;
  readonly defeatedByIsChampion: boolean;
}
