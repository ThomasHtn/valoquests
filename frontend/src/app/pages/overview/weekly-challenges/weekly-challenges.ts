import { Component, computed, inject } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { LucideTarget } from '@lucide/angular';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { CollapsibleCard } from '@shared/collapsible-card/collapsible-card';
import { PointsBadge } from '@shared/points-badge/points-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { ChallengeRow } from './weekly-challenges.model';

/**
 * "Weekly challenges" card of the overview page.
 *
 * Displays the collective completion progress of every challenge selected for the active week.
 */
@Component({
  selector: 'app-weekly-challenges',
  imports: [
    TranslatePipe,
    ChallengeIconView,
    CollapsibleCard,
    MatTooltip,
    PointsBadge,
    ProgressBar,
    ResourceState,
    LucideTarget,
  ],
  templateUrl: './weekly-challenges.html',
})
export class WeeklyChallenges {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Challenges of the active week, paired with their resolved icon and color treatment.
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
    })),
  );

  /**
   * Number of challenges every tracked player has completed, shown as a summary in the card header.
   *
   * A challenge counts as done only once the whole group has cleared it, since the card reports
   * collective rather than individual progress.
   */
  protected readonly completedCount = computed(
    () =>
      this.rows().filter((row) => row.totalPlayers > 0 && row.completedPlayers === row.totalPlayers)
        .length,
  );
}
