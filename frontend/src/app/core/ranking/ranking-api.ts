import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { CurrentRanking } from './ranking.model';

/**
 * Data-access service for the weekly player ranking.
 */
@Service()
export class RankingApi {
  /**
   * Current weekly ranking, with each player's exact progress toward every active challenge.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/rankings/current`.
   */
  public readonly current = httpResource<CurrentRanking>(() => '/api/rankings/current');
}
