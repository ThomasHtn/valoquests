import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

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
  public readonly players = httpResource<readonly PlayerSummary[]>(() => '/api/players', {
    defaultValue: [],
  });
}
