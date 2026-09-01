import { BossCategory } from './boss.model';
import { BossTimelineNodeStatus } from './boss-timeline.constants';

/**
 * One player's share of the damage dealt to a single week's boss, display-ready.
 *
 * Reconstructed from that week's ranking (the total damage a player scored during the week is
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
   * Damage dealt to that week's boss, as the raw figure. Kept alongside {@link damageLabel} because
   * one reader plots it rather than printing it: the profile's run frieze scales ten weeks against
   * the tallest of them, which a formatted string cannot be measured on.
   */
  readonly damage: number;

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
 * The player who landed a boss's last hit point.
 */
export interface BossFinishingBlow {
  readonly displayName: string;

  /**
   * Their portrait, read off the week's own ranking rows — the boss history carries the name and the
   * identifier, not the picture.
   */
  readonly avatarUrl: string | null;
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

  /**
   * Monday beginning this week, as `YYYY-MM-DD`, or `null` for an `'upcoming'` placeholder whose
   * week isn't determined yet.
   *
   * Carried separately from {@link id} even though a real node's id holds the same string: this one
   * is a week key meant to be handed to another screen — the ranking's `?week=` deep link — and a
   * placeholder's synthetic id must never end up there.
   */
  readonly weekStart: string | null;

  readonly status: BossTimelineNodeStatus;

  /**
   * Position of the week inside its run, from one. This is what the campaign map joins on to write
   * a week's colony reward on its own hexagon — never the node's position in the list, which a week
   * that closed without a fight would shift.
   */
  readonly runWeekIndex: number;

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

  /**
   * The raw weight class behind {@link categoryLabel}, kept alongside the translated string so the
   * map can color the active week's icon by difficulty without re-deriving it from the label.
   */
  readonly category: BossCategory | null;

  /**
   * The weight class this week is *scheduled* to fight, which is known for every week of the run
   * including the ones whose boss has not been drawn — the class comes from the calendar, only the
   * opponent comes from the draw.
   *
   * Distinct from {@link category}, which is the class of the boss actually standing there and stays
   * `null` until it is drawn. This one is what lets the map mark the two elite peaks weeks ahead.
   */
  readonly scheduledCategory: BossCategory | null;

  /**
   * Translated name of {@link scheduledCategory}, `null` outside the run's ten weeks.
   */
  readonly scheduledCategoryLabel: string | null;

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
   * The complement of {@link hpPercentage}: the share of the boss's hit points already taken off,
   * from 0 to 100. Held alongside rather than derived at render time because the battle map fills
   * each territory hexagon by ground *gained*, while the timeline drains a bar by hit points left.
   */
  readonly damagePercentage: number;

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
   * Who put the boss down, `null` on every week that did not settle with a kill.
   *
   * The one individual statistic of a week that the ranking cannot give: it names a moment, not a
   * total, which is why the campaign panel is where it belongs.
   */
  readonly finishingBlow: BossFinishingBlow | null;

  /**
   * The week's damage broken down per player, ordered best first. Empty for an `'upcoming'`
   * placeholder and for any week the ranking does not cover.
   */
  readonly contributions: readonly BossContribution[];
}
