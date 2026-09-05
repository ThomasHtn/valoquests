import { httpResource } from '@angular/common/http';
import { Service, signal } from '@angular/core';

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
   * Whether a screen has asked for the catalogue. Idle until then: the catalogue is a reserve of
   * over a hundred entries folded away on the challenges page, and nothing else reads it.
   */
  public readonly catalogueRequested = signal(false);

  /**
   * Every challenge the draws pick from, outside of any one week's draw — what "the pool" this
   * week's five were drawn from can still hand out.
   *
   * Shared as a single reactive resource for the same reason as {@link current}, and only fetched
   * once {@link catalogueRequested} is raised.
   */
  public readonly catalogue = httpResource<ChallengeCatalogue>(() =>
    this.catalogueRequested() ? API_ENDPOINTS.challengeCatalogue : undefined,
  );
}
