import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { Colony, ColonyRunHistory, ColonyTrajectory } from './colony.model';

/**
 * Data-access service for the squad's shared colony.
 *
 * All three resources are shared at service level, like `BossApi`: none of them takes a parameter
 * — the colony is one object, the curve is the run in progress, and the history is every closed
 * run — so every consumer reads the same in-flight request.
 */
@Service()
export class ColonyApi {
  /**
   * The colony as it stands today.
   */
  public readonly colony = httpResource<Colony>(() => API_ENDPOINTS.colony);

  /**
   * Population curve of the run in progress.
   */
  public readonly trajectory = httpResource<ColonyTrajectory>(() => API_ENDPOINTS.colonyTrajectory);

  /**
   * Every closed run, most recent first.
   */
  public readonly history = httpResource<readonly ColonyRunHistory[]>(
    () => API_ENDPOINTS.colonyHistory,
    { defaultValue: [] },
  );
}
