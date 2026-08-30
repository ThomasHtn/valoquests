import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { ChallengeCatalogue, CurrentChallenges } from './challenge.model';

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

  /**
   * Every challenge eligible for weekly selection, outside of any one week's draw — what "the
   * pool" this week's five were drawn from can still hand out.
   *
   * Shared as a single reactive resource for the same reason as {@link current}.
   */
  public readonly catalogue = httpResource<ChallengeCatalogue>(
    () => API_ENDPOINTS.challengeCatalogue,
  );
}
