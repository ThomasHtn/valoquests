import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';
import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';
import { PageResponse } from '@core/http/page-response.model';
import { CurrentRanking, DailyRanking, RankingHistoryWeek } from './ranking.model';
import { RANKING_HISTORY_MAX_WEEKS } from './ranking-api.constants';

/**
 * Data access for the player rankings.
 */
@Service()
export class RankingApi {
  /**
   * Current weekly ranking with challenge progress.
   */
  public readonly current = httpResource<CurrentRanking>(() => API_ENDPOINTS.currentRanking);

  /**
   * Last finalized week, which resolves the reigning "Champion" title.
   */
  public readonly latestFinalizedWeek = httpResource<PageResponse<RankingHistoryWeek>>(() => ({
    url: API_ENDPOINTS.rankingHistory,
    params: { page: 0, size: 1 },
  }));

  /**
   * Every finalized week, newest first, fetched once so screens browse it client-side.
   */
  public readonly history = httpResource<PageResponse<RankingHistoryWeek>>(() => ({
    url: API_ENDPOINTS.rankingHistory,
    params: { page: 0, size: RANKING_HISTORY_MAX_WEEKS },
  }));

  /**
   * Today's ranking against yesterday; no `day` param so the backend's time zone picks today.
   */
  public readonly daily = httpResource<DailyRanking>(() => API_ENDPOINTS.dailyRanking);
}
