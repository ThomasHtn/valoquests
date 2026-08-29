import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Column header of the ranking table: a challenge paired with the visual treatment shared with the
 * weekly challenges card, so the two widgets read as one coherent color language.
 */
export interface RankingColumn {
  readonly challengeId: number;

  /**
   * The challenge's own name, e.g. `"Roi du Deathmatch"`. Read by assistive technology in the
   * matrix header, where a column is identified visually by its tier badge alone.
   */
  readonly name: string;

  /**
   * Short category label derived from the challenge's metric (e.g. `"Kills"`), shown instead of
   * the challenge's full name so the table header stays scannable at a glance. The full name is
   * still carried by {@link tooltip}.
   */
  readonly categoryLabel: string;

  /**
   * Pre-formatted target value, e.g. `"100"`, or `null` for composite challenges with no stored
   * target.
   *
   * Carried by the header rather than by each cell: the target is identical down the whole column,
   * so repeating it once per player is noise.
   */
  readonly targetLabel: string | null;

  /**
   * Tooltip text shown when hovering the column's header, combining the challenge's name and
   * description.
   *
   * Supplements what the header shows visually: the tier badge and the target. The name and
   * description it carries are also exposed to assistive technology through the header's
   * `sr-only` caption, so nothing is reachable by pointer only.
   */
  readonly tooltip: string;
  readonly visual: ChallengeVisual;
}

/**
 * Single per-challenge progress cell rendered in a ranking row.
 */
export interface RankingCell {
  readonly challengeId: number;

  /**
   * The challenge's own name, e.g. `"Roi du Deathmatch"`.
   *
   * Labels the cell in the stacked card layout used below `xl`, where the tier badges identifying
   * the columns are not rendered. The metric alone would not do: several of a week's five
   * challenges routinely share one metric, so a card would list "Matches" three times over.
   */
  readonly name: string;

  /**
   * Short category label derived from the challenge's metric (e.g. `"Kills"`), read by assistive
   * technology alongside the cell's value so the unit the number counts is announced with it.
   */
  readonly categoryLabel: string;

  /**
   * Pre-formatted current value, e.g. `"42"`.
   */
  readonly currentValueLabel: string;

  /**
   * Pre-formatted target value, e.g. `"100"`, or `null` for composite challenges with no stored
   * target, in which case only {@link currentValueLabel} is shown.
   */
  readonly targetValueLabel: string | null;
  readonly completionPercentage: number;

  /**
   * Whether the player has validated the challenge.
   *
   * Tracked separately from {@link completionPercentage}, which is clamped to 100: a player who
   * overshot the target and one who stopped exactly on it draw the same full bar, and a composite
   * challenge has no target to fill at all.
   */
  readonly completed: boolean;
  readonly visual: ChallengeVisual;
}

/**
 * Single row of the ranking table: a player paired with their resolved avatar and per-challenge
 * progress cells, aligned with {@link RankingColumn}.
 */
export interface RankingRow {
  /**
   * 1-based ranking position, or `null` for an inactive player: still shown for their individual
   * challenge progress, but never ranked and pinned to the bottom of the table.
   */
  readonly position: number | null;

  /**
   * Places gained since last week's ranking: positive when the player climbed, negative when they
   * dropped, zero when they held their place or have no previous position.
   */
  readonly positionVariation: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;

  /**
   * Pre-formatted damage dealt to the week's boss, grouped in the active language (`"12 400"`).
   * This is the amount the ranking is ordered on, bonuses included.
   */
  readonly damageLabel: string;

  /**
   * Pre-formatted share of {@link damageLabel} that came from the regularity and team bonuses,
   * signed (`"+2 350"`), or `null` when the player earned none.
   *
   * Broken out because the two are earned in completely different ways: damage is dealt by
   * playing, bonuses are granted for showing up regularly and for clearing a challenge alongside
   * the rest of the squad.
   */
  readonly bonusLabel: string | null;

  /**
   * Per-challenge progress, one cell per challenge drawn that week — empty on a finalized week,
   * where the backend only keeps the totals (see `RankingHistoryWeek`). An empty list is what tells
   * both layouts to fall back on {@link completedLabel} and {@link activeDaysLabel}.
   */
  readonly cells: readonly RankingCell[];

  /**
   * Number of challenges the player cleared that week, as text, or `null` on the live week — where
   * {@link cells} shows the same thing challenge by challenge.
   */
  readonly completedLabel: string | null;

  /**
   * Distinct days the player was active that week, as text, and `null` on the live week for the
   * same reason as {@link completedLabel}: it is a stand-in, not a second reading.
   */
  readonly activeDaysLabel: string | null;

  /**
   * Whether this player holds the reigning weekly "Champion" title, earned by finishing 1st in
   * the most recently finalized week.
   */
  readonly isChampion: boolean;
}
