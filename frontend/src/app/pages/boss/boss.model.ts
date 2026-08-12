import { BossTimelineNodeStatus } from './boss-timeline.constants';

/**
 * One player's share of the damage dealt to a single week's boss, display-ready.
 *
 * Reconstructed from that week's ranking (the challenge damage a player scored during the week is
 * exactly the damage the boss took from them), so it exists for every week the ranking covers —
 * finalized or active — and never for a week whose boss has not been drawn.
 */
export interface BossContribution {
  readonly playerId: number;

  /**
   * 1-based position within the week's ranking.
   */
  readonly position: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;

  /**
   * Whether this player is the reigning weekly "Champion", so their avatar carries the title's
   * gold ring here too.
   */
  readonly isChampion: boolean;

  /**
   * Damage dealt, grouped for reading (`21 400`).
   */
  readonly damageLabel: string;

  /**
   * Challenges cleared that week — `"4/5"` while the week is still running and its total is known,
   * the bare count once finalized, since the backend's history does not carry how many challenges
   * that week had.
   */
  readonly questsLabel: string;
}

/**
 * One marker on the boss campaign timeline, display-ready.
 *
 * Covers all four states the timeline renders: a finalized past week (`'defeated'` /
 * `'survived'`), the active week (`'current'`), or a locked placeholder for a week ahead
 * (`'upcoming'`) whose boss doesn't exist yet — see {@link BossTimelineNodeStatus}.
 *
 * Every label is baked here already translated and already formatted: the node is what both the
 * timeline and the detail panel render, and neither should have to re-derive the same string from
 * raw hit points twice.
 */
export interface BossTimelineNode {
  /**
   * Stable identity for the `@for` track expression and for the panel's selection: a real node's
   * ISO `weekStart`, or a synthetic key for an `'upcoming'` placeholder.
   */
  readonly id: string;

  readonly status: BossTimelineNodeStatus;

  /**
   * ISO week number shown inside the hex marker, or `null` for an `'upcoming'` placeholder whose
   * week isn't determined yet.
   */
  readonly weekNumber: number | null;

  /**
   * Full `Semaine 32` wording, used where the marker's bare number needs spelling out — the row's
   * accessible name.
   */
  readonly weekLabel: string | null;
  readonly dateRangeLabel: string | null;

  /**
   * Translated status pill wording (`Vaincu`, `En cours`, …).
   */
  readonly statusLabel: string;

  readonly bossName: string;
  readonly bossDescription: string;

  /**
   * `null` for an `'upcoming'` placeholder, whose boss category isn't drawn yet.
   */
  readonly categoryLabel: string | null;
  readonly portraitUrl: string | null;

  /**
   * Whether the week has a boss to report damage on at all, i.e. every status but `'upcoming'`.
   * Read by both the timeline card and the panel to pick between the damage block and the locked
   * placeholder.
   */
  readonly hasDamage: boolean;

  /**
   * Share of the boss's effective hit points still standing, from 0 to 100, floored at 0 once the
   * boss is down. Zero for an `'upcoming'` placeholder.
   */
  readonly hpPercentage: number;

  /**
   * That same share as text (`36 %`), and the raw tally behind it (`33 600 / 95 000 PV`).
   */
  readonly hpPercentageLabel: string;
  readonly hpLabel: string;

  /**
   * Translated caption above the health bar — see `resolveBossHpBarLabelKey`.
   */
  readonly barLabel: string;

  /**
   * The one line qualifying the week: who landed the finishing blow, who dealt the most damage, or
   * how long the active week has left. `null` when nothing is known yet.
   */
  readonly metaLabel: string | null;

  /**
   * The week's damage broken down per player, ordered best first. Empty for an `'upcoming'`
   * placeholder and for any week the ranking does not cover.
   */
  readonly contributions: readonly BossContribution[];
}
