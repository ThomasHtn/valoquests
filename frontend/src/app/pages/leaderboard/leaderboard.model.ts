import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { TitleVisual } from '@core/campaign/titles/campaign-title-visual.model';
import { StreakPip } from '@shared/streak-gauge/streak-gauge.model';

/**
 * Title held on the board, with its icon and colour.
 */
export interface BoardTitle extends TitleVisual {
  /**
   * Weekly title.
   */
  readonly key: WeeklyTitle;

  /**
   * Worded figure the title was awarded on ("2 996 composants"), `null` if not kept.
   */
  readonly measure: string | null;
}

/**
 * Weekly attendance, as the streak gauge draws it.
 */
export interface BoardStreak {
  /**
   * Monday to Sunday pips, `null` when only the count is known (closed week).
   */
  readonly week: readonly StreakPip[] | null;

  /**
   * Days played this week.
   */
  readonly days: number;

  /**
   * Streak bonus in percent: today's, or what playing today would earn.
   */
  readonly bonusPercent: number;
}

/**
 * Board row.
 */
export interface BoardRow {
  /**
   * Player id.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Portrait URL, `null` for the fallback icon.
   */
  readonly portrait: string | null;

  /**
   * One-based position, `null` for an operator out of the campaign (tracked, never ranked).
   */
  readonly position: number | null;

  /**
   * Places climbed since the last calculation, negative when lost, zero on a closed week.
   */
  readonly variation: number;

  /**
   * Whether the operator is the reigning champion.
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
   * Ranking points from challenges.
   */
  readonly challengePoints: number;

  /**
   * Highest-priority title held, `null` when none.
   */
  readonly title: BoardTitle | null;

  /**
   * Matches played this week.
   */
  readonly matchCount: number;

  /**
   * Weekly attendance.
   */
  readonly streak: BoardStreak;

  /**
   * Challenges validated this week, weekly and daily.
   */
  readonly challengesCompleted: number;

  /**
   * Challenges the week draws: the weekly ones plus one daily per day.
   */
  readonly challengesMax: number;
}

/**
 * Week the board shows: the live one or a closed one.
 */
export interface BoardWeek {
  /**
   * Monday of the week (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Whether this is the week in progress.
   */
  readonly live: boolean;

  /**
   * One-based week in the campaign, `null` outside one.
   */
  readonly weekIndex: number | null;

  /**
   * Rows with a position.
   */
  readonly ranked: readonly BoardRow[];

  /**
   * Rows without a position.
   */
  readonly unranked: readonly BoardRow[];
}

/**
 * Winner of a closed week, as the picker names it.
 */
export interface WeekWinner {
  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Portrait URL, `null` for the fallback icon.
   */
  readonly portrait: string | null;
}

/**
 * Week offered by the picker.
 */
export interface WeekOption {
  /**
   * Monday of the week (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Formatted Monday to Sunday span.
   */
  readonly label: string;

  /**
   * One-based week in its campaign, `null` outside one.
   */
  readonly index: number | null;

  /**
   * Campaign id, `null` outside one, so the picker can rule off each run.
   */
  readonly group: number | null;

  /**
   * Whether this is the week in progress.
   */
  readonly live: boolean;

  /**
   * Week winner, `null` while running or when nobody was ranked.
   */
  readonly winner: WeekWinner | null;
}
