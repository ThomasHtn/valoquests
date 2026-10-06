import { inject, Service } from '@angular/core';

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
   * Reloads every shared resource, so all screens reflect the latest backend state.
   */
  public reload(): void {
    reloadAll(
      this.synchronizationApi.status,
      this.playersApi.players,
      this.campaignApi.campaign,
      this.campaignApi.today,
      this.campaignApi.history,
      this.rankingApi.current,
      this.rankingApi.latestFinalizedWeek,
      this.rankingApi.history,
      this.rankingApi.daily,
      this.challengesApi.current,
    );
  }
}
