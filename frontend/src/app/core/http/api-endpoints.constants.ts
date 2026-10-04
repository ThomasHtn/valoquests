import { environment } from '@env/environment';

/**
 * Backend endpoints, resolved against the build's `apiBaseUrl`.
 */
export const API_ENDPOINTS = {
  /**
   * `GET` every tracked player's summary.
   */
  players: `${environment.apiBaseUrl}/players`,

  /**
   * `GET` one player's profile and statistics.
   */
  playerDetails: (playerId: number): string => `${environment.apiBaseUrl}/players/${playerId}`,

  /**
   * `GET` one player's paginated match history.
   */
  playerMatches: (playerId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/matches`,

  /**
   * `GET` one player-match detail (`matchId` is the player-match id).
   */
  playerMatchDetail: (playerId: number, matchId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/matches/${matchId}`,

  /**
   * `GET` the roster's paginated matches of the day.
   */
  squadMatches: `${environment.apiBaseUrl}/matches`,

  /**
   * `GET` one player's progression analytics.
   */
  playerProgression: (playerId: number): string =>
    `${environment.apiBaseUrl}/players/${playerId}/progression`,

  /**
   * `GET` every known season.
   */
  seasons: `${environment.apiBaseUrl}/seasons`,

  /**
   * `GET` whether a synchronization runs and when the last one finished.
   */
  synchronizationStatus: `${environment.apiBaseUrl}/synchronization/status`,

  /**
   * `GET` the active week's challenges.
   */
  currentChallenges: `${environment.apiBaseUrl}/challenges/current`,

  /**
   * `GET` every challenge eligible for the weekly draw.
   */
  challengeCatalogue: `${environment.apiBaseUrl}/challenges/catalogue`,

  /**
   * `GET` the current weekly ranking.
   */
  currentRanking: `${environment.apiBaseUrl}/rankings/current`,

  /**
   * `GET` the paginated finalized weekly rankings.
   */
  rankingHistory: `${environment.apiBaseUrl}/rankings/history`,

  /**
   * `GET` one day's ranking against the day before (today by default).
   */
  dailyRanking: `${environment.apiBaseUrl}/rankings/daily`,

  /**
   * `GET` the live campaign, else the last closed one.
   */
  campaign: `${environment.apiBaseUrl}/campaign`,

  /**
   * `GET` today's gains, operator by operator.
   */
  campaignToday: `${environment.apiBaseUrl}/campaign/today`,

  /**
   * `GET` every closed campaign and how it ended.
   */
  campaignHistory: `${environment.apiBaseUrl}/campaign/history`,

  /**
   * Admin routes, all guarded by the `X-Admin-Key` header.
   */
  admin: {
    /**
     * `GET` a check that the admin key is accepted.
     */
    session: `${environment.apiBaseUrl}/admin/session`,

    /**
     * `POST` a background synchronization of every player.
     */
    synchronizations: `${environment.apiBaseUrl}/admin/synchronizations`,

    /**
     * `GET` the latest synchronization execution.
     */
    latestSynchronization: `${environment.apiBaseUrl}/admin/synchronizations/latest`,

    /**
     * `GET` a page of past synchronizations, newest first.
     */
    synchronizationHistory: `${environment.apiBaseUrl}/admin/synchronizations`,

    /**
     * `GET` one synchronization with its per-player results.
     */
    synchronization: (synchronizationId: number): string =>
      `${environment.apiBaseUrl}/admin/synchronizations/${synchronizationId}`,

    /**
     * `POST` a background synchronization of one player.
     */
    playerSynchronization: (playerId: number): string =>
      `${environment.apiBaseUrl}/admin/players/${playerId}/synchronizations`,

    /**
     * `POST` a fresh draw of the current week's challenges.
     */
    challengeRedraw: `${environment.apiBaseUrl}/admin/challenges/current/redraw`,

    /**
     * `POST` a rebuild of the current weekly ranking.
     */
    rankingRecalculation: `${environment.apiBaseUrl}/admin/rankings/recalculation`,

    /**
     * `POST` the weekly rollover, run now.
     */
    weeklyRollover: `${environment.apiBaseUrl}/admin/weeks/rollover`,

    /**
     * `GET` every player including archived ones, `POST` to add one.
     */
    players: `${environment.apiBaseUrl}/admin/players`,

    /**
     * `PUT` a player's identity, `DELETE` to remove it.
     */
    player: (playerId: number): string => `${environment.apiBaseUrl}/admin/players/${playerId}`,

    /**
     * `PATCH` a player's lifecycle status.
     */
    playerStatus: (playerId: number): string =>
      `${environment.apiBaseUrl}/admin/players/${playerId}/status`,

    /**
     * `POST` an irreversible wipe of everything derived from match history.
     */
    campaignReset: `${environment.apiBaseUrl}/admin/maintenance/campaign-reset`,

    /**
     * `POST` a campaign opening at a difficulty and starting Monday.
     */
    campaigns: `${environment.apiBaseUrl}/admin/campaigns`,

    /**
     * `POST` a stop of the live campaign, frozen at yesterday's base.
     */
    campaignStop: `${environment.apiBaseUrl}/admin/campaigns/stop`,

    /**
     * `POST` the nightly tick, run now (idempotent).
     */
    campaignTick: `${environment.apiBaseUrl}/admin/campaigns/tick`,

    /**
     * `DELETE` one campaign with its weeks, roster and snapshots.
     */
    campaign: (campaignId: number): string =>
      `${environment.apiBaseUrl}/admin/campaigns/${campaignId}`,
  },
} as const;
