import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';
import { GameMode } from '@core/matches/game-mode/match-game-mode.model';

import { PlayerDetails } from './player-details.model';
import { PlayerProgression } from './progression/player-progression.model';
import { PlayerSummary } from './player-summary.model';

/**
 * Data-access service for tracked players.
 */
@Service()
export class PlayersApi {
  /**
   * Every tracked player's summary, shared so consumers reuse one request.
   */
  public readonly players = httpResource<readonly PlayerSummary[]>(() => API_ENDPOINTS.players, {
    defaultValue: [],
  });

  /**
   * Player profile over every mode and season, idle while `id` is `null`.
   */
  public details(id: Signal<number | null>): HttpResourceRef<PlayerDetails | undefined> {
    return httpResource<PlayerDetails>(() => {
      const playerId = id();
      return playerId === null ? undefined : API_ENDPOINTS.playerDetails(playerId);
    });
  }

  /**
   * Profile scoped to one mode and optional season, apart so filters never refetch the banner.
   * Idle without a mode: an every-mode aggregate would mix incomparable queues.
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
   * Progression analytics over `seasonIds` (empty for all), idle while `id` is `null`.
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
        // Repeated params bind to Spring's `List<Long>`; omitted when empty, meaning every season.
        params: { ...(selectedSeasonIds.length > 0 ? { seasonIds: [...selectedSeasonIds] } : {}) },
      };
    });
  }
}
