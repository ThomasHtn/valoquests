import { environment } from '@env/environment';

/**
 * Backend endpoints consumed by the application, resolved against the configured
 * {@link Environment.apiBaseUrl}.
 *
 * Centralized so the REST contract is described in one place and the base URL stays configurable
 * per build configuration, rather than being repeated as a literal in every data-access service.
 */
export const API_ENDPOINTS = {
  /**
   * `GET` every tracked player's compact summary.
   */
  players: `${environment.apiBaseUrl}/players`,

  /**
   * `GET` one tracked player's detailed profile and aggregated statistics.
   *
   * @param playerId - Internal player identifier.
   * @returns The endpoint URL.
   */
  playerDetails: (playerId: number): string => `${environment.apiBaseUrl}/players/${playerId}`,

  /**
   * `GET` one tracked player's paginated match history.
   *
   * @param playerId - Internal player identifier.
   * @returns The endpoint URL.
   */
  playerMatches: (playerId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/matches`,

  /**
   * `GET` every known season, used to filter match history.
   */
  seasons: `${environment.apiBaseUrl}/seasons`,

  /**
   * `GET` the challenges selected for the active calendar week.
   */
  currentChallenges: `${environment.apiBaseUrl}/challenges/current`,

  /**
   * `GET` the current weekly ranking.
   */
  currentRanking: `${environment.apiBaseUrl}/rankings/current`,

  /**
   * `GET` the paginated history of finalized weekly rankings.
   */
  rankingHistory: `${environment.apiBaseUrl}/rankings/history`,
} as const;
