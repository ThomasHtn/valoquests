import { GameMode } from '@core/matches/game-mode/match-game-mode.model';
import { StatVisual } from '@core/players/stats/player-stats.model';

/**
 * Profile views: match history and multi-season progression.
 */
export type ProfileView = 'MATCHES' | 'PROGRESS';

/**
 * Season scope from the address: a season id, every season, or `null` for the current one.
 */
export type SeasonParam = number | 'ALL' | null;

/**
 * Profile state kept in the address, so a reload or a step back restores it.
 */
export interface ProfileQuery {
  /**
   * Open view.
   */
  readonly view: ProfileView;

  /**
   * History game mode, `null` for every mode.
   */
  readonly mode: GameMode | null;

  /**
   * History season scope.
   */
  readonly season: SeasonParam;
}

/**
 * One button of the display switch.
 */
export interface ProfileViewOption {
  /**
   * View the button opens.
   */
  readonly mode: ProfileView;

  /**
   * Translation key of its label.
   */
  readonly labelKey: string;
}

/**
 * Stat strip of the filtered matches, formatted; every figure reads `—` without a match.
 */
export interface StatStrip {
  /**
   * Win rate in percent, the bar's fill.
   */
  readonly winRate: number;

  /**
   * Formatted win rate.
   */
  readonly winRateLabel: string;

  /**
   * Win rate colour, shared by its text and bar.
   */
  readonly winRateVisual: StatVisual;

  /**
   * Matches won.
   */
  readonly wins: number;

  /**
   * Matches lost.
   */
  readonly losses: number;

  /**
   * Formatted KDA.
   */
  readonly kdaLabel: string;

  /**
   * KDA colour.
   */
  readonly kdaTone: string;

  /**
   * Formatted headshot rate.
   */
  readonly headshotPercentageLabel: string;

  /**
   * Formatted average damage per round.
   */
  readonly adrLabel: string;

  /**
   * Formatted average combat score.
   */
  readonly acsLabel: string;

  /**
   * Matches in the selection.
   */
  readonly matchesPlayed: number;
}
