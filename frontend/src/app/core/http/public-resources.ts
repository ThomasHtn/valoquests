import { effect, inject, ResourceRef, Service, signal, untracked } from '@angular/core';

import { CampaignApi } from '@core/campaign/campaign-api';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { SynchronizationApi } from '@core/synchronization/synchronization-api';

import { reloadAll } from './resource-state.utils';

/**
 * The shared public resources, reloaded together whenever backend data may have changed.
 */
@Service()
export class PublicResources {
  /**
   * Synchronization API, whose status drives the live refresh.
   */
  private readonly synchronizationApi = inject(SynchronizationApi);

  /**
   * Players API, whose roster admin edits change.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Campaign API, whose campaign, day and history are rebuilt by a replay.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Ranking API, whose rankings follow every synchronization.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Challenges API, whose current challenges follow every synchronization.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Every shared resource but the synchronization status.
   */
  private readonly dataResources: readonly ResourceRef<unknown>[] = [
    this.playersApi.players,
    this.campaignApi.campaign,
    this.campaignApi.today,
    this.campaignApi.history,
    this.rankingApi.current,
    this.rankingApi.latestFinalizedWeek,
    this.rankingApi.history,
    this.rankingApi.daily,
    this.challengesApi.current,
  ];

  /**
   * Values shown before a running {@link refresh}, put back if their reload fails.
   */
  private readonly keptValues = signal<ReadonlyMap<ResourceRef<unknown>, unknown>>(new Map());

  /**
   * Whether the last {@link refresh} left some screen on its previous value.
   */
  private readonly stale = signal(false);

  /**
   * Number of reloads so far, so page-local resources can follow them.
   */
  private readonly reloadCount = signal(0);

  /**
   * Read-only view of {@link stale}.
   */
  public readonly isStale = this.stale.asReadonly();

  /**
   * Read-only view of {@link reloadCount}.
   */
  public readonly revision = this.reloadCount.asReadonly();

  constructor() {
    // A failed background reload keeps the screen as it was rather than showing an error.
    effect(() => {
      const settled = [...this.keptValues()].filter(([resource]) => !resource.isLoading());
      if (settled.length === 0) {
        return;
      }

      untracked(() => {
        settled.forEach(([resource, value]) => {
          if (resource.error() !== undefined) {
            resource.set(value);
            this.stale.set(true);
          }
        });
        this.keptValues.update(
          (kept) => new Map([...kept].filter(([resource]) => resource.isLoading())),
        );
      });
    });
  }

  /**
   * Reloads every shared resource, so all screens reflect the latest backend state.
   */
  public reload(): void {
    reloadAll(this.synchronizationApi.status, ...this.dataResources);
    this.reloadCount.update((count) => count + 1);
  }

  /**
   * Flags a page-local resource left on its previous value, so the next poll refreshes again.
   */
  public markStale(): void {
    this.stale.set(true);
  }

  /**
   * Background variant of {@link reload}: skips the status just read and keeps shown values on failure.
   */
  public refresh(): void {
    this.stale.set(false);
    this.keptValues.set(
      new Map(
        this.dataResources
          .filter((resource) => resource.hasValue())
          .map((resource) => [resource, resource.value()]),
      ),
    );
    reloadAll(...this.dataResources);
    this.reloadCount.update((count) => count + 1);
  }
}
