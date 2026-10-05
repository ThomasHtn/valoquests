import { httpResource, HttpClient, HttpResourceRef } from '@angular/common/http';
import { inject, Service, Signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { CampaignDifficulty, CampaignStartWeek } from '@core/campaign/campaign.model';
import { PageResponse } from '@core/http/page-response.model';
import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';
import { PublicResources } from '@core/http/public-resources';
import { reloadAll } from '@core/http/resource-state.utils';
import { ADMIN_KEY_HEADER } from './session/admin-session.constants';
import { AdminSession } from './session/admin-session';
import {
  AdminPlayer,
  AdminPlayerCreateRequest,
  AdminPlayerDeletionResult,
  AdminPlayerStatus,
  AdminPlayerUpdateRequest,
} from './players/admin-player.model';
import { CampaignAdmin } from './campaigns/admin-campaign.model';
import {
  SynchronizationDetails,
  SynchronizationExecution,
} from './synchronization/admin-synchronization.model';
import { SYNCHRONIZATION_HISTORY_PAGE_SIZE } from './admin-api.constants';

/**
 * Administration API: reads through `httpResource`, one-shot commands through `HttpClient`.
 */
@Service()
export class AdminApi {
  /**
   * HTTP client for commands.
   */
  private readonly http = inject(HttpClient);

  /**
   * Session gating every resource fetch.
   */
  private readonly session = inject(AdminSession);

  /**
   * Public resources, refreshed with the admin ones so the public screens see admin edits.
   */
  private readonly publicResources = inject(PublicResources);

  /**
   * Every tracked player, archived ones included.
   * Gated on the session: an early 401 would make the interceptor end a session not yet started.
   */
  public readonly players = httpResource<readonly AdminPlayer[]>(
    () => (this.session.isAuthenticated() ? API_ENDPOINTS.admin.players : undefined),
    { defaultValue: [] },
  );

  /**
   * Latest synchronization, `undefined` if none ran; polled by `refresh` while in flight.
   */
  public readonly latestSynchronization = httpResource<SynchronizationExecution>(() =>
    this.session.isAuthenticated() ? API_ENDPOINTS.admin.latestSynchronization : undefined,
  );

  /**
   * Page of past synchronizations, most recent first (`page` is zero-based).
   */
  public synchronizationHistory(
    page: Signal<number>,
  ): HttpResourceRef<PageResponse<SynchronizationExecution> | undefined> {
    return httpResource<PageResponse<SynchronizationExecution>>(() =>
      this.session.isAuthenticated()
        ? {
            url: API_ENDPOINTS.admin.synchronizationHistory,
            params: { page: page(), size: SYNCHRONIZATION_HISTORY_PAGE_SIZE },
          }
        : undefined,
    );
  }

  /**
   * One synchronization with its per-player outcomes, idle while the id is `null`.
   */
  public synchronizationDetails(
    synchronizationId: Signal<number | null>,
  ): HttpResourceRef<SynchronizationDetails | undefined> {
    return httpResource<SynchronizationDetails>(() => {
      const id = synchronizationId();

      return this.session.isAuthenticated() && id !== null
        ? API_ENDPOINTS.admin.synchronization(id)
        : undefined;
    });
  }

  /**
   * Verifies a key not yet in the session; rejects when the backend refuses it.
   * The explicit header makes `adminKeyInterceptor` leave the request alone.
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
   */
  public async synchronizeAllPlayers(): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.synchronizations, null));
  }

  /**
   * Starts a background synchronization of one tracked player.
   */
  public async synchronizePlayer(playerId: number): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.playerSynchronization(playerId), null));
  }

  /**
   * Redraws the current week's challenges; destructive, their progress is deleted.
   */
  public async redrawCurrentChallenges(): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.challengeRedraw, null));
  }

  /**
   * Runs the weekly rollover now.
   */
  public async runWeeklyRollover(): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.weeklyRollover, null));
  }

  /**
   * Runs the daily tick now.
   */
  public async runDailyTick(): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.campaignTick, null));
  }

  /**
   * Adds a player to the tracked roster.
   */
  public async createPlayer(request: AdminPlayerCreateRequest): Promise<AdminPlayer> {
    return this.mutate(this.http.post<AdminPlayer>(API_ENDPOINTS.admin.players, request));
  }

  /**
   * Updates a tracked player's identity.
   */
  public async updatePlayer(
    playerId: number,
    request: AdminPlayerUpdateRequest,
  ): Promise<AdminPlayer> {
    return this.mutate(this.http.put<AdminPlayer>(API_ENDPOINTS.admin.player(playerId), request));
  }

  /**
   * Changes a player's lifecycle status (also restores an archived one).
   */
  public async changePlayerStatus(
    playerId: number,
    status: AdminPlayerStatus,
  ): Promise<AdminPlayer> {
    return this.mutate(
      this.http.patch<AdminPlayer>(API_ENDPOINTS.admin.playerStatus(playerId), { status }),
    );
  }

  /**
   * Removes a player by deletion or archiving; resolves with what was done.
   */
  public async removePlayer(playerId: number): Promise<AdminPlayerDeletionResult> {
    return this.mutate(
      this.http.delete<AdminPlayerDeletionResult>(API_ENDPOINTS.admin.player(playerId)),
    );
  }

  /**
   * Irreversibly clears every record derived from match history.
   */
  public async resetCampaign(): Promise<void> {
    await this.mutate(this.http.post(API_ENDPOINTS.admin.campaignReset, null));
  }

  /**
   * Opens a campaign on the active roster at the given difficulty and start week.
   */
  public async openCampaign(
    difficulty: CampaignDifficulty,
    startWeek: CampaignStartWeek,
  ): Promise<CampaignAdmin> {
    return this.mutate(
      this.http.post<CampaignAdmin>(API_ENDPOINTS.admin.campaigns, null, {
        params: { difficulty, startWeek },
      }),
    );
  }

  /**
   * Stops the live campaign, frozen at yesterday's base.
   */
  public async stopCampaign(): Promise<CampaignAdmin> {
    return this.mutate(this.http.post<CampaignAdmin>(API_ENDPOINTS.admin.campaignStop, null));
  }

  /**
   * Deletes a campaign with its weeks, roster and snapshots.
   */
  public async deleteCampaign(campaignId: number): Promise<void> {
    await this.mutate(this.http.delete<void>(API_ENDPOINTS.admin.campaign(campaignId)));
  }

  /**
   * Refetches every admin resource; also the polling step, the backend cannot push progress.
   */
  public refresh(): void {
    reloadAll(this.players, this.latestSynchronization);
    this.publicResources.reload();
  }

  /**
   * Sends a command then refreshes every resource; the single cache-invalidation point.
   */
  private async mutate<T>(request$: Observable<T>): Promise<T> {
    const result = await firstValueFrom(request$);

    this.refresh();

    return result;
  }
}
