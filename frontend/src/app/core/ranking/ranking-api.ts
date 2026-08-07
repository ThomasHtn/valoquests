import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { PageResponse } from '@core/http/page-response.model';
import { CurrentRanking, RankingHistoryWeek } from './ranking.model';

/**
 * Upper bound of finalized weeks fetched in one call to {@link RankingApi.history} — the backend's
 * own maximum for `size` on `GET /api/rankings/history`.
 *
 * The tracked group is fixed and the calendar cadence is weekly (see the root CLAUDE.md), so almost
 * two years of history stay under this ceiling. Fetching it all in one request lets the ranking
 * history page's carousel and quick-jump dropdown browse weeks entirely client-side instead of
 * round-tripping on every navigation step.
 */
const RANKING_HISTORY_MAX_WEEKS = 100;

/**
 * Data-access service for the weekly player ranking.
 */
@Service()
export class RankingApi {
  /**
   * Current weekly ranking, with each player's exact progress toward every active challenge.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/rankings/current`.
   */
  public readonly current = httpResource<CurrentRanking>(() => API_ENDPOINTS.currentRanking);

  /**
   * Most recently finalized week's ranking, used to resolve the reigning "Champion" title (see
   * {@link resolveChampionPlayerId}).
   *
   * Shared as a single reactive resource for the same reason as {@link current}: every screen
   * decorating a player's name with the title reads the same in-flight request.
   */
  public readonly latestFinalizedWeek = httpResource<PageResponse<RankingHistoryWeek>>(() => ({
    url: API_ENDPOINTS.rankingHistory,
    params: { page: 0, size: 1 },
  }));

  /**
   * Every finalized weekly ranking, ordered from the most recent completed week to the oldest.
   *
   * Shared as a single reactive resource, like {@link current}: the ranking history page browses
   * it entirely client-side (its carousel and quick-jump dropdown alike), so one request covers
   * the whole page instead of one per navigation step.
   */
  public readonly history = httpResource<PageResponse<RankingHistoryWeek>>(() => ({
    url: API_ENDPOINTS.rankingHistory,
    params: { page: 0, size: RANKING_HISTORY_MAX_WEEKS },
  }));
}
