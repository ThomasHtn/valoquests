import { httpResource } from '@angular/common/http';
import { Service, signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';

import { ChallengeCatalogue, CurrentChallenges } from './challenge.model';

/**
 * Challenge data access.
 */
@Service()
export class ChallengesApi {
  /**
   * Active week's challenges with squad progress.
   */
  public readonly current = httpResource<CurrentChallenges>(() => API_ENDPOINTS.currentChallenges);

  /**
   * Whether a screen asked for the catalogue, a large folded-away list left idle until then.
   */
  public readonly catalogueRequested = signal(false);

  /**
   * Every challenge the draws pick from, fetched once `catalogueRequested` is raised.
   */
  public readonly catalogue = httpResource<ChallengeCatalogue>(() =>
    this.catalogueRequested() ? API_ENDPOINTS.challengeCatalogue : undefined,
  );
}
