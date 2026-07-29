import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { Season } from './season.model';

/**
 * Data-access service for seasons available to filter match history.
 */
@Service()
export class SeasonsApi {
  /**
   * Every known season, most recently discovered first.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/seasons`.
   */
  public readonly seasons = httpResource<readonly Season[]>(() => API_ENDPOINTS.seasons, {
    defaultValue: [],
  });
}
