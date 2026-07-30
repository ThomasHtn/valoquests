import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';
import { GameMode } from '@core/matches/game-mode.model';

import { PlayerDetails } from './player-details.model';
import { PlayerSummary } from './player-summary.model';

/**
 * Data-access service for tracked players.
 */
@Service()
export class PlayersApi {
  /**
   * Every tracked player's compact summary.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/players`.
   */
  public readonly players = httpResource<readonly PlayerSummary[]>(() => API_ENDPOINTS.players, {
    defaultValue: [],
  });

  /**
   * Detailed profile and aggregated statistics of one tracked player, scoped to one game mode and,
   * optionally, one season.
   *
   * Created per caller, unlike {@link players}, since it is parameterized by the requested player.
   *
   * @param id - Reactive internal player identifier.
   * @param gameMode - Reactive game mode the statistics are scoped to.
   * @param seasonId - Reactive season filter, or `null` to include every season.
   * @returns The reactive resource fetching the requested player's detailed profile.
   */
  public details(
    id: Signal<number>,
    gameMode: Signal<GameMode>,
    seasonId: Signal<number | null>,
  ): HttpResourceRef<PlayerDetails | undefined> {
    return httpResource<PlayerDetails>(() => {
      const selectedSeasonId = seasonId();

      return {
        url: API_ENDPOINTS.playerDetails(id()),
        params: {
          gameMode: gameMode(),
          ...(selectedSeasonId !== null ? { seasonId: selectedSeasonId } : {}),
        },
      };
    });
  }
}
