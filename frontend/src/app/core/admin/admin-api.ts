import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_ENDPOINTS } from '@core/http/api-endpoints';

import { ADMIN_KEY_HEADER } from './admin-session.constants';
import { AdminSession } from './admin-session';
import {
  AdminPlayer,
  AdminPlayerCreateRequest,
  AdminPlayerDeletionResult,
  AdminPlayerStatus,
  AdminPlayerUpdateRequest,
  SynchronizationExecution,
} from './admin.model';

/**
 * Data-access service for the administration API.
 *
 * Reads go through `httpResource`, like every other screen of the application. Commands go through
 * `HttpClient` instead: `httpResource` describes a value the UI observes, which a one-shot
 * destructive action is not.
 */
@Service()
export class AdminApi {
  /**
   * HTTP client used by the command operations.
   */
  private readonly http = inject(HttpClient);

  /**
   * Session deciding whether the reactive resources may fetch at all.
   */
  private readonly session = inject(AdminSession);

  /**
   * Every tracked player, archived ones included.
   *
   * Held back until a session is open. The sign-in screen injects this service to verify a key, and
   * a resource fetching on construction would fire an unauthenticated request whose 401 the
   * interceptor would answer by ending a session that had not started.
   */
  public readonly players = httpResource<readonly AdminPlayer[]>(
    () => (this.session.isAuthenticated() ? API_ENDPOINTS.admin.players : undefined),
    { defaultValue: [] },
  );

  /**
   * Most recent synchronization execution, or `undefined` when none has ever run.
   *
   * Reloaded by {@link refresh} while a run is in flight, which is the only way the backoffice can
   * follow a walk that outlives the request that started it. Gated on the session like
   * {@link players}.
   */
  public readonly latestSynchronization = httpResource<SynchronizationExecution>(() =>
    this.session.isAuthenticated() ? API_ENDPOINTS.admin.latestSynchronization : undefined,
  );

  /**
   * Verifies an administrator key against the backend.
   *
   * The key is sent explicitly rather than through the session, since the whole point is to test
   * one the session does not hold yet. `adminKeyInterceptor` leaves requests carrying the header
   * alone, so a rejection surfaces here instead of ending a session that never opened.
   *
   * @param key - The administrator key to verify.
   * @returns A promise that resolves when the key is accepted, and rejects otherwise.
   */
  public async verifyKey(key: string): Promise<void> {
    await firstValueFrom(
      this.http.get(API_ENDPOINTS.admin.session, {
        headers: { [ADMIN_KEY_HEADER]: key },
        observe: 'response',
      }),
    );
  }

  /**
   * Starts a background synchronization of every tracked player.
   *
   * @returns A promise that resolves once the run has been accepted.
   */
  public async synchronizeAllPlayers(): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.synchronizations, null));
    this.refresh();
  }

  /**
   * Starts a background synchronization of one tracked player.
   *
   * @param playerId - Internal player identifier.
   * @returns A promise that resolves once the run has been accepted.
   */
  public async synchronizePlayer(playerId: number): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.playerSynchronization(playerId), null));
    this.refresh();
  }

  /**
   * Rebuilds the current week's challenge progress and the weekly ranking.
   *
   * @returns A promise that resolves once the rebuild has completed.
   */
  public async recalculateProgress(): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.challengeRecalculation, null));
  }

  /**
   * Rebuilds the current weekly ranking alone, without touching challenge progress.
   *
   * @returns A promise that resolves once the rebuild has completed.
   */
  public async recalculateRanking(): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.rankingRecalculation, null));
  }

  /**
   * Draws the current week's missing challenges and its boss encounter.
   *
   * @returns A promise that resolves once the week is fully set up.
   */
  public async selectCurrentWeek(): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.currentWeekSelection, null));
  }

  /**
   * Adds a player to the tracked roster.
   *
   * @param request - The player's identity.
   * @returns A promise that resolves with the created player.
   */
  public async createPlayer(request: AdminPlayerCreateRequest): Promise<AdminPlayer> {
    const created = await firstValueFrom(
      this.http.post<AdminPlayer>(API_ENDPOINTS.admin.players, request),
    );

    this.refresh();

    return created;
  }

  /**
   * Updates the identity of a tracked player.
   *
   * @param playerId - Internal player identifier.
   * @param request - The new identity.
   * @returns A promise that resolves with the updated player.
   */
  public async updatePlayer(
    playerId: number,
    request: AdminPlayerUpdateRequest,
  ): Promise<AdminPlayer> {
    const updated = await firstValueFrom(
      this.http.put<AdminPlayer>(API_ENDPOINTS.admin.player(playerId), request),
    );

    this.refresh();

    return updated;
  }

  /**
   * Moves a tracked player to another lifecycle status, which is also how an archived player is
   * restored.
   *
   * @param playerId - Internal player identifier.
   * @param status - The status to apply.
   * @returns A promise that resolves with the updated player.
   */
  public async changePlayerStatus(
    playerId: number,
    status: AdminPlayerStatus,
  ): Promise<AdminPlayer> {
    const updated = await firstValueFrom(
      this.http.patch<AdminPlayer>(API_ENDPOINTS.admin.playerStatus(playerId), { status }),
    );

    this.refresh();

    return updated;
  }

  /**
   * Removes a player from the roster, by deletion or by archiving.
   *
   * @param playerId - Internal player identifier.
   * @returns A promise that resolves with what the request actually did.
   */
  public async removePlayer(playerId: number): Promise<AdminPlayerDeletionResult> {
    const result = await firstValueFrom(
      this.http.delete<AdminPlayerDeletionResult>(API_ENDPOINTS.admin.player(playerId)),
    );

    this.refresh();

    return result;
  }

  /**
   * Irreversibly clears every record derived from match history.
   *
   * @returns A promise that resolves once the reset has completed.
   */
  public async resetCampaign(): Promise<void> {
    await firstValueFrom(this.http.post(API_ENDPOINTS.admin.campaignReset, null));
    this.refresh();
  }

  /**
   * Refetches every administration resource.
   *
   * A command is a `POST` sent beside the resources, so nothing they depend on changes and they
   * would otherwise keep describing the state from before it. This is also the polling step: a run
   * in flight is followed by calling this on an interval, since the backend has no way to push its
   * progress.
   */
  public refresh(): void {
    this.players.reload();
    this.latestSynchronization.reload();
  }
}
