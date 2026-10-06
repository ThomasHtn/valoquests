import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { TitleVisual } from '@core/campaign/titles/campaign-title-visual.model';
import { StreakPip } from '@shared/streak-gauge/streak-gauge.model';

/**
 * One operator's day on the squad sheet.
 */
export interface SquadRow {
  /**
   * Position on the weekly board, `null` when unranked.
   */
  readonly position: number | null;

  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Bundled agent portrait name, `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Whether the operator holds the reigning Champion title.
   */
  readonly champion: boolean;

  /**
   * Weekly title held, with its visual, or `null`.
   */
  readonly title: (TitleVisual & { readonly key: WeeklyTitle }) | null;

  /**
   * Whether the operator played today.
   */
  readonly played: boolean;

  /**
   * Today's streak bonus in percent, or what playing would earn.
   */
  readonly streakBonusPercent: number;

  /**
   * Days of the week played so far, today included once played.
   */
  readonly playedDays: number;

  /**
   * Monday to Sunday, one pip per day.
   */
  readonly streakWeek: readonly StreakPip[];

  /**
   * Guardian damage dealt today.
   */
  readonly damage: number;

  /**
   * Matches played.
   */
  readonly matchCount: number;

  /**
   * Matches paid below full value.
   */
  readonly reducedMatchCount: number;

  /**
   * Components gained today.
   */
  readonly components: number;

  /**
   * Food gained today.
   */
  readonly food: number;
}

/**
 * Column of the sheet's header, as translation keys.
 */
export interface SquadColumn {
  /**
   * Key of the column name.
   */
  readonly label: string;

  /**
   * Key of the tooltip explaining the column.
   */
  readonly tooltip: string;
}
