import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service, Signal } from '@angular/core';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { PageResponse } from '@core/http/page-response.model';
import { BossHistoryWeek, CurrentBoss } from './boss.model';

/**
 * Number of finalized weeks requested per page of boss history.
 */
const BOSS_HISTORY_PAGE_SIZE = 5;

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
   * Finalized weekly boss confrontations, paginated by week and ordered from the most recent
   * completed week to the oldest.
   *
   * Created per caller, unlike {@link current}, since it is parameterized by the requested page.
   *
   * @param page - Reactive zero-based page index.
   * @returns The reactive resource fetching the requested page of boss history.
   */
  public history(page: Signal<number>): HttpResourceRef<PageResponse<BossHistoryWeek> | undefined> {
    return httpResource<PageResponse<BossHistoryWeek>>(() => ({
      url: API_ENDPOINTS.bossHistory,
      params: { page: page(), size: BOSS_HISTORY_PAGE_SIZE },
    }));
  }
}
