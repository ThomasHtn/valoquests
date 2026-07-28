import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { PageResponse } from '../shared/page-response.model';
import { MATCH_HISTORY_PAGE_SIZE } from './matches-api.constants';
import { Match } from './match.model';
import { MatchResult } from './match-result.model';

/**
 * Data-access service for tracked players' match history.
 */
@Service()
export class MatchesApi {
  /**
   * Filtered and paginated match history for one tracked player.
   *
   * Created per caller since it is parameterized by the requested player, page and filters.
   *
   * @param playerId - Reactive internal player identifier.
   * @param page - Reactive zero-based page index.
   * @param result - Reactive result filter, or `null` to include every result.
   * @param seasonId - Reactive season filter, or `null` to include every season.
   * @returns The reactive resource fetching the requested page of match history.
   */
  public history(
    playerId: Signal<number>,
    page: Signal<number>,
    result: Signal<MatchResult | null>,
    seasonId: Signal<number | null>,
  ): HttpResourceRef<PageResponse<Match> | undefined> {
    return httpResource<PageResponse<Match>>(() => {
      const selectedResult = result();
      const selectedSeasonId = seasonId();

      return {
        url: `/api/players/${playerId()}/matches`,
        params: {
          page: page(),
          size: MATCH_HISTORY_PAGE_SIZE,
          ...(selectedResult ? { result: selectedResult } : {}),
          ...(selectedSeasonId !== null ? { seasonId: selectedSeasonId } : {}),
        },
      };
    });
  }
}
