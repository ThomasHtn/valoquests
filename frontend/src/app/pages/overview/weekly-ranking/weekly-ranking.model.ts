import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Column header of the ranking table: a challenge paired with the visual treatment shared with the
 * weekly challenges card, so the two widgets read as one coherent color language.
 */
export interface RankingColumn {
  readonly challengeId: number;

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
   * Supplements the visible name with the challenge's description; it is no longer the only place
   * the name exists, so it may stay out of reach of keyboard and touch.
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
   * Short category label derived from the challenge's metric (e.g. `"Kills"`), shown as the
   * cell's label in the stacked card layout used below `lg`, where the table header carrying it
   * is not rendered.
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
  readonly position: number;

  /**
   * Places gained since last week's ranking: positive when the player climbed, negative when they
   * dropped, zero when they held their place or have no previous position.
   */
  readonly positionVariation: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly points: number;
  readonly cells: readonly RankingCell[];

  /**
   * Whether this player holds the reigning weekly "Champion" title, earned by finishing 1st in
   * the most recently finalized week.
   */
  readonly isChampion: boolean;
}
