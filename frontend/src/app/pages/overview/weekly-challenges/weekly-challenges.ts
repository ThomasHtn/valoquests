import { Component, computed, inject } from '@angular/core';
import { Tooltip } from '@shared/tooltip/tooltip';
import { LucideCheck, LucideTarget } from '@lucide/angular';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { RankingEntry } from '@core/ranking/ranking.model';
import { Avatar } from '@shared/avatar/avatar';
import { PointsBadge } from '@shared/points-badge/points-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { ChallengeRow } from './weekly-challenges.model';

/**
 * "Weekly challenges" card of the overview page.
 *
 * Displays the collective completion progress of every challenge selected for the active week, one
 * vignette per challenge.
 */
@Component({
  selector: 'app-weekly-challenges',
  imports: [
    TranslatePipe,
    ChallengeIconView,
    Tooltip,
    PointsBadge,
    Avatar,
    ResourceState,
    LucideTarget,
    LucideCheck,
  ],
  templateUrl: './weekly-challenges.html',
})
export class WeeklyChallenges {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Data-access service backing the shared current-ranking resource, used to build each
   * challenge's avatar stack.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Reactive resource fetching the current week's ranking, shared with the podium and weekly
   * ranking card.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.challengesResource, this.rankingResource);

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = anyError(this.challengesResource, this.rankingResource);

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Every tracked player's current ranking entry, used to resolve per-challenge completion.
   */
  private readonly rankingEntries = computed<readonly RankingEntry[]>(
    () => resourceValue(this.rankingResource, null)?.ranking ?? [],
  );

  /**
   * Challenges of the active week, paired with their resolved icon and color treatment and each
   * player's completion for that specific challenge.
   */
  protected readonly rows = computed<readonly ChallengeRow[]>(() =>
    (resourceValue(this.challengesResource, null)?.challenges ?? []).map((challenge) => ({
      id: challenge.id,
      name: challenge.name,
      description: challenge.description,
      difficulty: challenge.difficulty,
      completedPlayers: challenge.completedPlayers,
      totalPlayers: challenge.totalPlayers,
      completionPercentage: challenge.completionPercentage,
      points: challenge.points,
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
      contributors: this.rankingEntries().map((entry) => ({
        playerId: entry.player.id,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        contributed:
          entry.challengeProgress.find((progress) => progress.challengeId === challenge.id)
            ?.completed ?? false,
      })),
    })),
  );

  /**
   * Reloads both backing resources after a failure.
   */
  protected reload(): void {
    reloadAll(this.challengesResource, this.rankingResource);
  }
}
