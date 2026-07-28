import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { PageResponse } from '../shared/page-response.model';
import { CurrentRanking, RankingHistoryWeek } from './ranking.model';

/**
 * Number of finalized weeks requested per page of ranking history.
 */
const RANKING_HISTORY_PAGE_SIZE = 5;

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
  public readonly current = httpResource<CurrentRanking>(() => '/api/rankings/current');

  /**
   * Finalized weekly rankings, paginated by week and ordered from the most recent completed week
   * to the oldest.
   *
   * Created per caller, unlike {@link current}, since it is parameterized by the requested page.
   *
   * @param page - Reactive zero-based page index.
   * @returns The reactive resource fetching the requested page of ranking history.
   */
  public history(
    page: Signal<number>,
  ): HttpResourceRef<PageResponse<RankingHistoryWeek> | undefined> {
    return httpResource<PageResponse<RankingHistoryWeek>>(() => ({
      url: '/api/rankings/history',
      params: { page: page(), size: RANKING_HISTORY_PAGE_SIZE },
    }));
  }
}
