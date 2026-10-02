import { GameMode } from '@core/matches/game-mode.model';

/**
 * The profile's two views: the match history and the multi-season progression.
 */
export type ProfileView = 'MATCHES' | 'PROGRESS';

/**
 * Season scope read from the address: a season, every season, or nothing said (the current one).
 */
export type SeasonParam = number | 'ALL' | null;

/**
 * The profile's state as kept in the address, so a reload or a step back restores it.
 */
export interface ProfileQuery {
  /**
   * The open view.
   */
  readonly view: ProfileView;

  /**
   * The match history's game mode, `null` for every mode.
   */
  readonly mode: GameMode | null;

  /**
   * The match history's season scope.
   */
  readonly season: SeasonParam;
}
