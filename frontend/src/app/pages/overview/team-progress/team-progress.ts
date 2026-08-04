import { Component, computed, inject, input } from '@angular/core';
import { LucideCheck, LucideClock } from '@lucide/angular';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { Avatar } from '@shared/avatar/avatar';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { WeekSummary } from '../overview.model';

/**
 * Single contributor shown in the team hero's avatar stack.
 */
interface Contributor {
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;

  /**
   * Whether the player has completed at least one challenge this week.
   */
  readonly contributed: boolean;
}

/**
 * Team objective banner of the overview page.
 *
 * Reframes the weekly challenges as a collective goal rather than an individual one: the
 * proportion of challenges the whole group has already cleared, who has contributed so far, and
 * the time left to finish together. Reads the same shared current-challenges and current-ranking
 * resources as `WeeklyChallenges` and `WeeklyRanking` directly, rather than reaching into either
 * component's internals, so both stay unchanged.
 */
@Component({
  selector: 'app-team-progress',
  imports: [TranslatePipe, Avatar, ProgressBar, ResourceState, LucideCheck, LucideClock],
  templateUrl: './team-progress.html',
})
export class TeamProgress {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Active week summary, computed once by `Overview` from a single ticking clock so the countdown
   * shown here never drifts from the page header.
   */
  public readonly week = input<WeekSummary | null>(null);

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header and
   * the weekly challenges card.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Reactive resource fetching the current week's ranking, shared with the weekly ranking card.
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
   * Challenges selected for the active week.
   */
  private readonly challenges = computed(
    () => resourceValue(this.challengesResource, null)?.challenges ?? [],
  );

  /**
   * Total number of challenges selected for the active week.
   */
  protected readonly totalCount = computed(() => this.challenges().length);

  /**
   * Number of challenges every tracked player has completed.
   *
   * Mirrors `WeeklyChallenges.completedCount`: a challenge counts as done only once the whole
   * group has cleared it, since this banner reports collective rather than individual progress.
   */
  protected readonly completedCount = computed(
    () =>
      this.challenges().filter(
        (challenge) =>
          challenge.totalPlayers > 0 && challenge.completedPlayers === challenge.totalPlayers,
      ).length,
  );

  /**
   * Share of the week's challenges the group has fully cleared, from 0 to 100.
   */
  protected readonly progressPercentage = computed(() => {
    const total = this.totalCount();
    return total === 0 ? 0 : Math.round((this.completedCount() / total) * 100);
  });

  /**
   * Every active tracked player, paired with whether they have completed at least one challenge
   * this week, shown as the hero's avatar stack.
   *
   * An inactive player is excluded: this banner reports collective progress, which never counts
   * an inactive player's completions. The backend omits `position` from the JSON entirely when
   * null, so the parsed value is `undefined` rather than `null` - `!= null` catches both.
   */
  protected readonly contributors = computed<readonly Contributor[]>(() =>
    (resourceValue(this.rankingResource, null)?.ranking ?? [])
      .filter((entry) => entry.position != null)
      .map((entry) => ({
        playerId: entry.player.id,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        contributed: entry.completedChallenges > 0,
      })),
  );

  /**
   * Number of players who have contributed to at least one challenge this week.
   */
  protected readonly contributorsCount = computed(
    () => this.contributors().filter((contributor) => contributor.contributed).length,
  );

  /**
   * Reloads both backing resources after a failure.
   */
  protected reload(): void {
    reloadAll(this.challengesResource, this.rankingResource);
  }
}
