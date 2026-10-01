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
   * Detailed profile of one tracked player, with statistics over every mode and season.
   *
   * Created per caller, unlike {@link players}, since it is parameterized by the requested player.
   *
   * @param id - Reactive internal player identifier, or `null` when the route names none, which
   *   leaves the resource idle.
   * @returns The reactive resource fetching the requested player's detailed profile.
   */
  public details(id: Signal<number | null>): HttpResourceRef<PlayerDetails | undefined> {
    return httpResource<PlayerDetails>(() => {
      const playerId = id();
      return playerId === null ? undefined : API_ENDPOINTS.playerDetails(playerId);
    });
  }

  /**
   * Same profile as {@link details}, with its statistics scoped to one game mode and, optionally,
   * one season.
   *
   * Kept apart from {@link details} so a filter change only refetches the figures, never the
   * identity banner.
   *
   * @param id - Reactive internal player identifier, or `null` when the route names none.
   * @param gameMode - Reactive game mode the statistics are scoped to, or `null` to leave the
   *   resource idle: an every-mode aggregate would mix incomparable queues.
   * @param seasonId - Reactive season filter, or `null` to include every season.
   * @returns The reactive resource fetching the scoped profile.
   */
  public scopedDetails(
    id: Signal<number | null>,
    gameMode: Signal<GameMode | null>,
    seasonId: Signal<number | null>,
  ): HttpResourceRef<PlayerDetails | undefined> {
    return httpResource<PlayerDetails>(() => {
      const playerId = id();
      const selectedGameMode = gameMode();

      if (playerId === null || selectedGameMode === null) {
        return undefined;
      }

      const selectedSeasonId = seasonId();

      return {
        url: API_ENDPOINTS.playerDetails(playerId),
        params: {
          gameMode: selectedGameMode,
          ...(selectedSeasonId !== null ? { seasonId: selectedSeasonId } : {}),
        },
      };
    });
  }

  /**
   * Analytics behind one tracked player's progression view, scoped to a set of seasons.
   *
   * @param id - Reactive internal player identifier, or `null` while the player to load is not
   *   known yet, which leaves the resource idle instead of requesting an invalid identifier.
   * @param seasonIds - Reactive season selection; an empty list covers every season.
   * @returns The reactive resource fetching the requested player's progression analytics.
   */
  public progression(
    id: Signal<number | null>,
    seasonIds: Signal<readonly number[]>,
  ): HttpResourceRef<PlayerProgression | undefined> {
    return httpResource<PlayerProgression>(() => {
      const playerId = id();

      if (playerId === null) {
        return undefined;
      }

      const selectedSeasonIds = seasonIds();

      return {
        url: API_ENDPOINTS.playerProgression(playerId),
        // Repeated rather than comma-joined, which is how Spring binds a `List<Long>` parameter.
        // Omitted entirely when empty, which the backend reads as "every season".
        params: { ...(selectedSeasonIds.length > 0 ? { seasonIds: [...selectedSeasonIds] } : {}) },
      };
    });
  }
}
