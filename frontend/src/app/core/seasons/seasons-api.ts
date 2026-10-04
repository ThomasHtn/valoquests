import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';

import { Season } from './season.model';

/**
 * Seasons available to filter match history.
 */
@Service()
export class SeasonsApi {
  /**
   * Every known season, newest first.
   */
  public readonly seasons = httpResource<readonly Season[]>(() => API_ENDPOINTS.seasons, {
    defaultValue: [],
  });
}
