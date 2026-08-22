import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';
import { GameMode } from '@core/matches/game-mode.model';

import { PlayerDetails } from './player-details.model';
import { PlayerProgression } from './player-progression.model';
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

  /**
   * Analytics behind one tracked player's progression view, scoped to a set of seasons.
   *
   * @param id - Reactive internal player identifier.
   * @param seasonIds - Reactive season selection; an empty list covers every season.
   * @returns The reactive resource fetching the requested player's progression analytics.
   */
  public progression(
    id: Signal<number>,
    seasonIds: Signal<readonly number[]>,
  ): HttpResourceRef<PlayerProgression | undefined> {
    return httpResource<PlayerProgression>(() => {
      const selectedSeasonIds = seasonIds();

      return {
        url: API_ENDPOINTS.playerProgression(id()),
        // Repeated rather than comma-joined, which is how Spring binds a `List<Long>` parameter.
        // Omitted entirely when empty, which the backend reads as "every season".
        params: { ...(selectedSeasonIds.length > 0 ? { seasonIds: [...selectedSeasonIds] } : {}) },
      };
    });
  }
}
