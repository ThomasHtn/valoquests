import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';
import { API_ENDPOINTS } from '@core/http/api-endpoints';
import { PageResponse } from '@core/http/page-response.model';
import { CurrentRanking, DailyRanking, RankingHistoryWeek } from './ranking.model';
import { RANKING_HISTORY_MAX_WEEKS } from './ranking-api.constants';

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
   * Shared as a single reactive resource, like {@link current}: the leaderboard's week arrows and
   * the campaign's boss timeline both browse it client-side, so one request covers them both
   * instead of one per navigation step.
   */
  public readonly history = httpResource<PageResponse<RankingHistoryWeek>>(() => ({
    url: API_ENDPOINTS.rankingHistory,
    params: { page: 0, size: RANKING_HISTORY_MAX_WEEKS },
  }));

  /**
   * Today's ranking, and how it compares to yesterday.
   *
   * Asked for without a `day` parameter on purpose: the day the board means is the backend's own,
   * resolved against the rollover timezone `WeekCalendar` holds. Sending a date computed from the
   * visitor's clock would put a reader an hour ahead of that zone on tomorrow's empty board.
   */
  public readonly daily = httpResource<DailyRanking>(() => API_ENDPOINTS.dailyRanking);
}
