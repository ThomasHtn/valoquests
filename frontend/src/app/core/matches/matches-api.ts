import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';
import { API_ENDPOINTS } from '@core/http/api-endpoints';
import { PageResponse } from '@core/http/page-response.model';
import { GameMode } from './game-mode.model';
import { Match, MatchDetail } from './match.model';
import { MATCH_HISTORY_PAGE_SIZE, SQUAD_MATCH_PAGE_SIZE } from './matches-api.constants';
import { SquadMatch } from './squad-match.model';

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
   * @param playerId - Reactive internal player identifier, or `null` to leave the resource idle.
   * @param page - Reactive zero-based page index.
   * @param gameMode - Reactive game mode filter, or `null` to include every mode.
   * @param seasonId - Reactive season filter, or `null` to include every season.
   * @returns The reactive resource fetching the requested page of match history.
   */
  public history(
    playerId: Signal<number | null>,
    page: Signal<number>,
    gameMode: Signal<GameMode | null>,
    seasonId: Signal<number | null>,
  ): HttpResourceRef<PageResponse<Match> | undefined> {
    return httpResource<PageResponse<Match>>(() => {
      const id = playerId();
      if (id === null) {
        return undefined;
      }
      const selectedGameMode = gameMode();
      const selectedSeasonId = seasonId();

      return {
        url: API_ENDPOINTS.playerMatches(id),
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
   * Paginated matches of the day for the campaign roster, newest first.
   *
   * Created per caller, like {@link history}: parameterized by the requested page.
   *
   * @param page - Reactive zero-based page index.
   * @returns The reactive resource fetching the requested page of the squad's matches.
   */
  public squad(page: Signal<number>): HttpResourceRef<PageResponse<SquadMatch> | undefined> {
    return httpResource<PageResponse<SquadMatch>>(() => ({
      url: API_ENDPOINTS.squadMatches,
      params: { page: page(), size: SQUAD_MATCH_PAGE_SIZE },
    }));
  }

  /**
   * Full detail for one of a tracked player's matches.
   *
   * Created per caller, like {@link history}: parameterized by the requested player and match.
   *
   * @param playerId - Reactive internal player identifier, or `null` to leave the resource idle.
   * @param matchId - Reactive internal player-match identifier, or `null` while none is open.
   * @returns The reactive resource fetching the requested match's full detail.
   */
  public detail(
    playerId: Signal<number | null>,
    matchId: Signal<number | null>,
  ): HttpResourceRef<MatchDetail | undefined> {
    return httpResource<MatchDetail>(() => {
      const player = playerId();
      const id = matchId();

      return player === null || id === null
        ? undefined
        : API_ENDPOINTS.playerMatchDetail(player, id);
    });
  }
}
