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
   * `GET` the analytics behind one tracked player's progression view.
   *
   * @param playerId - Internal player identifier.
   * @returns The endpoint URL.
   */
  playerProgression: (playerId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/progression`,

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

  /**
   * `GET` the active week's boss confrontation.
   */
  currentBoss: `${environment.apiBaseUrl}/boss/current`,

  /**
   * `GET` the paginated history of finalized weekly boss confrontations.
   */
  bossHistory: `${environment.apiBaseUrl}/boss/history`,

  /**
   * Administration routes, every one of them guarded by the `X-Admin-Key` header that
   * `adminKeyInterceptor` attaches.
   *
   * Grouped under their own key so the public contract above stays readable as the list of what
   * the site itself consumes, and so nothing outside the backoffice reaches for one by accident.
   */
  admin: {
    /**
     * `GET` a confirmation that the supplied administrator key is accepted. Changes nothing.
     */
    session: `${environment.apiBaseUrl}/admin/session`,

    /**
     * `POST` a synchronization of every tracked player, run in the background.
     */
    synchronizations: `${environment.apiBaseUrl}/admin/synchronizations`,

    /**
     * `GET` the most recent synchronization execution, the only window on a background run.
     */
    latestSynchronization: `${environment.apiBaseUrl}/admin/synchronizations/latest`,

    /**
     * `POST` a synchronization of one tracked player, run in the background.
     *
     * @param playerId - Internal player identifier.
     * @returns The endpoint URL.
     */
    playerSynchronization: (playerId: number): string =>
      `${environment.apiBaseUrl}/admin/players/${playerId}/synchronizations`,

    /**
     * `POST` a rebuild of the current week's challenge progress and ranking.
     */
    challengeRecalculation: `${environment.apiBaseUrl}/admin/challenges/progress/recalculation`,

    /**
     * `POST` a rebuild of the current weekly ranking alone.
     */
    rankingRecalculation: `${environment.apiBaseUrl}/admin/rankings/recalculation`,

    /**
     * `POST` the selection of the current week's challenges and boss.
     */
    currentWeekSelection: `${environment.apiBaseUrl}/admin/weeks/current/selection`,

    /**
     * `GET` every player including archived ones, or `POST` one to add to the roster.
     */
    players: `${environment.apiBaseUrl}/admin/players`,

    /**
     * `PUT` a player's identity, or `DELETE` it from the roster.
     *
     * @param playerId - Internal player identifier.
     * @returns The endpoint URL.
     */
    player: (playerId: number): string => `${environment.apiBaseUrl}/admin/players/${playerId}`,

    /**
     * `PATCH` a player's lifecycle status.
     *
     * @param playerId - Internal player identifier.
     * @returns The endpoint URL.
     */
    playerStatus: (playerId: number): string =>
      `${environment.apiBaseUrl}/admin/players/${playerId}/status`,

    /**
     * `POST` an irreversible wipe of every record derived from match history.
     */
    campaignReset: `${environment.apiBaseUrl}/admin/maintenance/campaign-reset`,
  },
} as const;
