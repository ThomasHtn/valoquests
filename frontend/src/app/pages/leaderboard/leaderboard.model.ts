import { WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';
import { StreakPip } from '@shared/streak-gauge/streak-gauge.model';

/**
 * A title an operator holds on the board, with the icon and colour it is drawn in.
 */
export interface BoardTitle extends TitleVisual {
  /**
   * Which weekly title.
   */
  readonly key: WeeklyTitle;

  /**
   * The figure the title was awarded on, worded ("2 996 composants"), or `null` when the week
   * kept no such figure.
   */
  readonly measure: string | null;
}

/**
 * An operator's attendance over the week, as the streak gauge draws it.
 */
export interface BoardStreak {
  /**
   * The week from Monday to Sunday, or `null` when only the count is known (a closed week).
   */
  readonly week: readonly StreakPip[] | null;

  /**
   * Days of the week played.
   */
  readonly days: number;

  /**
   * Streak bonus in percent: today's, or the one playing today would earn while not played yet.
   */
  readonly bonusPercent: number;
}

/**
 * One row of the board: where the operator stands and what got them there.
 */
export interface BoardRow {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
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

  /**
   * Whether the operator holds the reigning Champion title.
   */
  readonly isChampion: boolean;

  /**
   * Total ranking points.
   */
  readonly total: number;

  /**
   * Guardian damage this week.
   */
  readonly damage: number;

  /**
   * Ranking points earned from challenges.
   */
  readonly challengePoints: number;

  /**
   * The one title this operator is decorated with, the highest-priority one held, or `null` when
   * they hold none.
   */
  readonly title: BoardTitle | null;

  /**
   * Matches played this week.
   */
  readonly matchCount: number;

  /**
   * Attendance over the week.
   */
  readonly streak: BoardStreak;

  /**
   * Challenges validated this week, weekly and daily alike.
   */
  readonly challengesCompleted: number;

  /**
   * Challenges the week draws: the weekly ones plus one daily per day.
   */
  readonly challengesMax: number;
}

/**
 * One week the board can show: the live one, or a closed one browsed back to.
 */
export interface BoardWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Whether this is the week in progress.
   */
  readonly live: boolean;

  /**
   * Position in the campaign, or `null` for a week outside one.
   */
  readonly weekIndex: number | null;

  /**
   * Rows with a position.
   */
  readonly ranked: readonly BoardRow[];

  /**
   * Rows without a position yet.
   */
  readonly unranked: readonly BoardRow[];
}

/**
 * The operator who finished a closed week first, as the week picker names it.
 */
export interface WeekWinner {
  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;
}

/**
 * One week the picker offers, newest first.
 */
export interface WeekOption {
  /**
   * Monday of the week, ISO date.
   */
  readonly weekStart: string;

  /**
   * Monday to Sunday, the month spelled once when both days share it.
   */
  readonly label: string;

  /**
   * Position in its campaign, or `null` for a week outside one.
   */
  readonly index: number | null;

  /**
   * What the option belongs to — a campaign's id, or `null` outside one — so the picker can rule
   * off one run of weeks from the next without naming them.
   */
  readonly group: number | null;

  /**
   * Whether this is the week in progress.
   */
  readonly live: boolean;

  /**
   * Who won the week; `null` while it is still running or when nobody was ranked.
   */
  readonly winner: WeekWinner | null;
}
