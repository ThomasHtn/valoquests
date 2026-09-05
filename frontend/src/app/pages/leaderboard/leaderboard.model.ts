import { WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';
import { ChallengeCadence } from '@core/challenges/challenge.model';
import { ChallengeTier } from '@core/challenges/challenge-visual.model';
import { ChallengeRingCell } from '@shared/challenge-ring/challenge-ring';

/**
 * A title an operator holds on the board, with the icon and colour it is drawn in.
 */
export interface BoardTitle extends TitleVisual {
  readonly key: WeeklyTitle;
}

/**
 * One operator's exact progress toward one challenge of the board, as a ring.
 */
export interface BoardRing extends ChallengeRingCell {
  readonly id: number;
  readonly cadence: ChallengeCadence;
  readonly mark: ChallengeTier;

  /**
   * The whole sentence the ring abbreviates: the name, where the operator stands, the points.
   */
  readonly tip: string;
}

/**
 * One row of the board: where the operator stands and what got them there.
 */
export interface BoardRow {
  readonly playerId: number;
  readonly name: string;
  readonly portrait: string | null;

  /**
   * 1-based position, or `null` for an operator out of the campaign, who is tracked but never
   * takes a slot.
   */
  readonly position: number | null;

  /**
   * Places climbed since the last calculation, negative when lost. Zero on a closed week.
   */
  readonly variation: number;
  readonly isChampion: boolean;
  readonly total: number;
  readonly damage: number;
  readonly challengePoints: number;
  readonly completedChallenges: number;
  readonly totalChallenges: number;
  readonly completedDaily: number;
  readonly streakDays: number;
  readonly activeDays: number;
  readonly titles: readonly BoardTitle[];

  /**
   * One ring per challenge on the board, or `null` on a closed week, whose progress was not kept.
   */
  readonly rings: readonly BoardRing[] | null;
}

/**
 * One week the board can show: the live one, or a closed one browsed back to.
 */
export interface BoardWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;
  readonly live: boolean;

  /**
   * Position in the campaign, or `null` for a week outside one.
   */
  readonly weekIndex: number | null;
  readonly ranked: readonly BoardRow[];
  readonly unranked: readonly BoardRow[];
}
