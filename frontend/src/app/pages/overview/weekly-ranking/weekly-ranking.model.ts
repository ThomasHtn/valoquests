import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Column header of the ranking table: a challenge paired with the visual treatment shared with the
 * weekly challenges card, so the two widgets read as one coherent color language.
 */
export interface RankingColumn {
  readonly challengeId: number;
  readonly name: string;

  /**
   * Tooltip text shown when hovering the column's icon, combining the challenge's name and
   * description.
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
   * Pre-formatted current value, e.g. `"42"`.
   */
  readonly currentValueLabel: string;

  /**
   * Pre-formatted target value, e.g. `"100"`, or `null` for composite challenges with no stored
   * target, in which case only {@link currentValueLabel} is shown.
   */
  readonly targetValueLabel: string | null;
  readonly completionPercentage: number;
  readonly visual: ChallengeVisual;
}

/**
 * Single row of the ranking table: a player paired with their resolved avatar and per-challenge
 * progress cells, aligned with {@link RankingColumn}.
 */
export interface RankingRow {
  readonly position: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly points: number;
  readonly cells: readonly RankingCell[];
}
