import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';

import { SynchronizationStatus } from './synchronization-status.model';

/**
 * Public synchronization status.
 */
@Service()
export class SynchronizationApi {
  /**
   * Running state and last completion, shared by the sidebar and the live refresh.
   */
  public readonly status = httpResource<SynchronizationStatus>(
    () => API_ENDPOINTS.synchronizationStatus,
  );
}
