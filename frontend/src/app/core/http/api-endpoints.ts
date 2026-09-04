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
   * `GET` full detail for one of a tracked player's matches.
   *
   * @param playerId - Internal player identifier.
   * @param matchId - Internal player-match identifier.
   * @returns The endpoint URL.
   */
  playerMatchDetail: (playerId: number, matchId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/matches/${matchId}`,

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
   * `GET` every challenge eligible for weekly selection, outside of any one week's draw.
   */
  challengeCatalogue: `${environment.apiBaseUrl}/challenges/catalogue`,

  /**
   * `GET` the current weekly ranking.
   */
  currentRanking: `${environment.apiBaseUrl}/rankings/current`,

  /**
   * `GET` the paginated history of finalized weekly rankings.
   */
  rankingHistory: `${environment.apiBaseUrl}/rankings/history`,

  /**
   * `GET` one day's ranking, and how it compares to the day before. Defaults to today.
   */
  dailyRanking: `${environment.apiBaseUrl}/rankings/daily`,

  /**
   * `GET` the rescue campaign the site shows: the live one, else the last closed one.
   */
  campaign: `${environment.apiBaseUrl}/campaign`,

  /**
   * `GET` what the squad brought in today, operator by operator.
   */
  campaignToday: `${environment.apiBaseUrl}/campaign/today`,

  /**
   * `GET` every closed campaign and how it ended.
   */
  campaignHistory: `${environment.apiBaseUrl}/campaign/history`,

  /**
   * `GET` one tracked player's contribution to the week and to the live campaign.
   *
   * @param playerId - Internal player identifier.
   * @returns The endpoint URL.
   */
  playerContribution: (playerId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/contribution`,

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
     * `GET` a page of past synchronization executions, most recent first.
     */
    synchronizationHistory: `${environment.apiBaseUrl}/admin/synchronizations`,

    /**
     * `GET` one synchronization execution with its per-player results.
     *
     * @param synchronizationId - Internal synchronization identifier.
     * @returns The endpoint URL.
     */
    synchronization: (synchronizationId: number): string =>
      `${environment.apiBaseUrl}/admin/synchronizations/${synchronizationId}`,

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
     * `POST` a fresh draw of the current week's challenges, discarding the pack it holds.
     */
    challengeRedraw: `${environment.apiBaseUrl}/admin/challenges/current/redraw`,

    /**
     * `POST` a rebuild of the current weekly ranking alone.
     */
    rankingRecalculation: `${environment.apiBaseUrl}/admin/rankings/recalculation`,

    /**
     * `POST` the selection of the current week's five challenges.
     */
    currentWeekSelection: `${environment.apiBaseUrl}/admin/weeks/current/selection`,

    /**
     * `POST` the whole weekly rollover, run now instead of on the next Monday.
     */
    weeklyRollover: `${environment.apiBaseUrl}/admin/weeks/rollover`,

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

    /**
     * `POST` the draw of today's daily challenge, when the nightly tick missed it.
     */
    dailyChallengeSelection: `${environment.apiBaseUrl}/admin/challenges/daily/selection`,

    /**
     * `GET` the calibration a campaign opened today would be given, or `POST` the opening itself.
     */
    campaigns: `${environment.apiBaseUrl}/admin/campaigns`,

    /**
     * `GET` the squad's measure without opening anything: reference, tier, per-operator coverage.
     */
    campaignCalibration: `${environment.apiBaseUrl}/admin/campaigns/calibration`,

    /**
     * `POST` a background import of every active operator's match history over the calibration
     * window, so the measure above stands on real weeks rather than on the last two acts.
     */
    campaignBackfill: `${environment.apiBaseUrl}/admin/campaigns/backfill`,

    /**
     * `POST` the stop of the live campaign, frozen at yesterday's base.
     */
    campaignStop: `${environment.apiBaseUrl}/admin/campaigns/stop`,

    /**
     * `POST` a replay of the running campaign from its first day. Idempotent: the base is never
     * advanced incrementally, so this rewrites exactly what a nightly tick would.
     */
    campaignReplay: `${environment.apiBaseUrl}/admin/campaigns/replay`,

    /**
     * `DELETE` one campaign with its weeks, roster and snapshots.
     *
     * @param campaignId - Internal campaign identifier.
     * @returns The campaign's endpoint.
     */
    campaign: (campaignId: number): string =>
      `${environment.apiBaseUrl}/admin/campaigns/${campaignId}`,
  },
} as const;
