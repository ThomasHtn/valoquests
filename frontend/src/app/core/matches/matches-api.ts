import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';
import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';
import { PageResponse } from '@core/http/page-response.model';
import { GameMode } from './game-mode/match-game-mode.model';
import { Match, MatchDetail } from './match.model';
import { MATCH_HISTORY_PAGE_SIZE, SQUAD_MATCH_PAGE_SIZE } from './matches-api.constants';
import { SquadMatch } from './match-squad.model';

/**
 * Data access for tracked players' match history.
 */
@Service()
export class MatchesApi {
  /**
   * Filtered history page of one player (zero-based `page`), idle while `playerId` is `null`.
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
   * Zero-based page of the roster's matches of the day, newest first.
   */
  public squad(page: Signal<number>): HttpResourceRef<PageResponse<SquadMatch> | undefined> {
    return httpResource<PageResponse<SquadMatch>>(() => ({
      url: API_ENDPOINTS.squadMatches,
      params: { page: page(), size: SQUAD_MATCH_PAGE_SIZE },
    }));
  }

  /**
   * Full detail of one player's match, idle while either identifier is `null`.
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
