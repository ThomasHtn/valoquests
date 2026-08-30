import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { PageResponse } from '@core/http/page-response.model';
import { GameMode } from './game-mode.model';
import { Match, MatchDetail } from './match.model';

/**
 * Number of matches requested per page of a player's match history.
 */
const MATCH_HISTORY_PAGE_SIZE = 10;

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
   * @param gameMode - Reactive game mode filter, or `null` to include every mode.
   * @param seasonId - Reactive season filter, or `null` to include every season.
   * @returns The reactive resource fetching the requested page of match history.
   */
  public history(
    playerId: Signal<number>,
    page: Signal<number>,
    gameMode: Signal<GameMode | null>,
    seasonId: Signal<number | null>,
  ): HttpResourceRef<PageResponse<Match> | undefined> {
    return httpResource<PageResponse<Match>>(() => {
      const selectedGameMode = gameMode();
      const selectedSeasonId = seasonId();

      return {
        url: API_ENDPOINTS.playerMatches(playerId()),
        params: {
          page: page(),
          size: MATCH_HISTORY_PAGE_SIZE,
          ...(selectedGameMode ? { gameMode: selectedGameMode } : {}),
          ...(selectedSeasonId !== null ? { seasonId: selectedSeasonId } : {}),
        },
      };
    });
  }

  /**
   * Full detail for one of a tracked player's matches.
   *
   * Created per caller, like {@link history}: parameterized by the requested player and match.
   *
   * @param playerId - Reactive internal player identifier.
   * @param matchId - Reactive internal player-match identifier, or `null` while none is open.
   * @returns The reactive resource fetching the requested match's full detail.
   */
  public detail(
    playerId: Signal<number>,
    matchId: Signal<number | null>,
  ): HttpResourceRef<MatchDetail | undefined> {
    return httpResource<MatchDetail>(() => {
      const id = matchId();

      return id === null ? undefined : API_ENDPOINTS.playerMatchDetail(playerId(), id);
    });
  }
}
