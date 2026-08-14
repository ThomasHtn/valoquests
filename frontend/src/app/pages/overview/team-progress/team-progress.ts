import { Component, computed, inject } from '@angular/core';

import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Single row of the collective progress breakdown: a challenge paired with its resolved
 * difficulty visual and how many tracked players have completed it so far.
 *
 * Styled with the same {@link ChallengeVisual} treatment as the weekly challenges board
 * (`Challenges`), so the two read as one system even though this list is a compact breakdown
 * rather than a full card grid.
 */
interface ChallengeProgressRow {
  readonly id: number;
  readonly name: string;
  readonly difficulty: ChallengeDifficulty;
  readonly completedPlayers: number;
  readonly totalPlayers: number;

  /**
   * Whether every tracked player has completed this challenge.
   */
  readonly completed: boolean;
  readonly visual: ChallengeVisual;
}

/**
 * Team objective band of the overview page.
 *
 * Reframes the weekly challenges as a collective goal rather than an individual one: the
 * proportion of challenges the whole group has already cleared. Reads the same shared
 * current-challenges resource as `Challenges` directly, rather than reaching into that
 * component's internals, so both stay unchanged.
 */
@Component({
  selector: 'app-team-progress',
  imports: [TranslatePipe, ResourceState],
  templateUrl: './team-progress.html',
  // Transparent host: the section itself becomes the grid item of the overview's two-column row,
  // so it stretches to the row's height and can align its bottom edge with the panel beside it.
  host: { class: 'contents' },
})
export class TeamProgress {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header and
   * the weekly challenges card.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Whether the backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.challengesResource);

  /**
   * Whether the backing resource failed to load.
   */
  protected readonly hasError = anyError(this.challengesResource);

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
   * The week's challenges, paired with their resolved difficulty visual and completion state —
   * the breakdown shown under the progress bar, styled like the weekly challenges board.
   */
  protected readonly rows = computed<readonly ChallengeProgressRow[]>(() =>
    this.challenges().map((challenge) => ({
      id: challenge.id,
      name: challenge.name,
      difficulty: challenge.difficulty,
      completedPlayers: challenge.completedPlayers,
      totalPlayers: challenge.totalPlayers,
      completed:
        challenge.totalPlayers > 0 && challenge.completedPlayers === challenge.totalPlayers,
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    })),
  );

  /**
   * Number of challenges every tracked player has completed.
   *
   * Mirrors `Challenges.completedCount`: a challenge counts as done only once the whole
   * group has cleared it, since this banner reports collective rather than individual progress.
   */
  protected readonly completedCount = computed(
    () => this.rows().filter((row) => row.completed).length,
  );

  /**
   * Number of challenges still awaiting a full clear, shown in the headline's deadline reminder.
   */
  protected readonly remainingCount = computed(() => this.totalCount() - this.completedCount());

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
   * Reloads the backing resource after a failure.
   */
  protected reload(): void {
    reloadAll(this.challengesResource);
  }
}
