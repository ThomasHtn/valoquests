import { GameMode } from '@core/matches/game-mode/match-game-mode.model';

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
