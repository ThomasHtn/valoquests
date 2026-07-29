import { Component, computed, inject } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { LucideTarget, LucideTrophy } from '@lucide/angular';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { CollapsibleCard } from '@shared/collapsible-card/collapsible-card';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
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
    ProgressBar,
    ResourceState,
    LucideTarget,
    LucideTrophy,
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
   * Challenges of the active week, paired with their resolved icon and color treatment.
   */
  protected readonly rows = computed<readonly ChallengeRow[]>(() =>
    (resourceValue(this.challengesResource, null)?.challenges ?? []).map((challenge) => ({
      id: challenge.id,
      name: challenge.name,
      description: challenge.description,
      completedPlayers: challenge.completedPlayers,
      totalPlayers: challenge.totalPlayers,
      completionPercentage: challenge.completionPercentage,
      points: challenge.points,
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    })),
  );
}
