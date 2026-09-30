import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { SynchronizationStatus } from './synchronization-status.model';

/**
 * Data-access service for the public synchronization status.
 */
@Service()
export class SynchronizationApi {
  /**
   * Whether a synchronization is running and when the last one finished.
   *
   * Shared so the sidebar label and the live refresh read the same poll.
   */
  public readonly status = httpResource<SynchronizationStatus>(
    () => API_ENDPOINTS.synchronizationStatus,
  );
}
