import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { PlayerSynchronizationStatus } from './player-summary.model';

/**
 * Data-access service for tracked players.
 */
@Service()
export class PlayersApi {
  /**
   * Every tracked player's synchronization status.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/players`.
   */
  public readonly players = httpResource<readonly PlayerSynchronizationStatus[]>(
    () => '/api/players',
    { defaultValue: [] },
  );
}
