import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { PageResponse } from '@core/http/page-response.model';
import { BossHistoryWeek, CurrentBoss } from './boss.model';

/**
 * Number of finalized weeks requested for the boss history, fetched in a single page. The battle
 * timeline (see the boss page) renders the group's whole confrontation history at once rather than
 * paginating it, and the group is small and short-lived enough (see the root CLAUDE.md) that this
 * ceiling — the backend's own maximum page size — is never expected to be reached.
 */
const BOSS_HISTORY_SIZE = 100;

/**
 * Data-access service for the weekly boss confrontation.
 */
@Service()
export class BossApi {
  /**
   * Active week's boss confrontation.
   *
   * Shared as a single reactive resource so every consumer reads the same in-flight request
   * instead of triggering its own call to `GET /api/boss/current`.
   */
  public readonly current = httpResource<CurrentBoss>(() => API_ENDPOINTS.currentBoss);

  /**
   * Every finalized weekly boss confrontation, ordered from the most recent completed week to the
   * oldest.
   *
   * Shared as a single reactive resource, like {@link current}: unlike the ranking/challenge
   * history pages, the boss timeline has no per-consumer parameter (no page to request) since it
   * always renders the full history.
   */
  public readonly history = httpResource<PageResponse<BossHistoryWeek>>(() => ({
    url: API_ENDPOINTS.bossHistory,
    params: { page: 0, size: BOSS_HISTORY_SIZE },
  }));
}
