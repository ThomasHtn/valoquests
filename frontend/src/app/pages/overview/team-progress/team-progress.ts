import { Component, computed, inject } from '@angular/core';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { Avatar } from '@shared/avatar/avatar';
import { ResourceState } from '@shared/resource-state/resource-state';

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
 * Team objective band of the overview page.
 *
 * Reframes the weekly challenges as a collective goal rather than an individual one: the
 * proportion of challenges the whole group has already cleared, and who has contributed so far.
 * Reads the same shared current-challenges and current-ranking resources as `Challenges` and
 * `Leaderboard` directly, rather than reaching into either component's internals, so both stay
 * unchanged.
 */
@Component({
  selector: 'app-team-progress',
  imports: [TranslatePipe, Avatar, ResourceState],
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
   * Mirrors `Challenges.completedCount`: a challenge counts as done only once the whole
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
   * One flag per challenge of the active week, `true` for the ones the group has already
   * cleared — driving the discrete, segmented progress bar (one segment per challenge) rather
   * than a continuous percentage fill, since {@link totalCount} is always a small, fixed number
   * of discrete challenges rather than a truly continuous quantity.
   */
  protected readonly segments = computed<readonly boolean[]>(() =>
    Array.from({ length: this.totalCount() }, (_, index) => index < this.completedCount()),
  );

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
