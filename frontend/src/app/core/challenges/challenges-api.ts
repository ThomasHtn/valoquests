import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { CurrentChallenges } from './challenge.model';

/**
 * Data-access service for the weekly challenge catalogue.
 */
@Service()
export class ChallengesApi {
  /**
   * Challenges selected for the active calendar week, with collective completion progress.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/challenges/current`.
   */
  public readonly current = httpResource<CurrentChallenges>(() => API_ENDPOINTS.currentChallenges);
}
