import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { LucideChevronDown, LucideTarget, LucideTrophy } from '@lucide/angular';

import { ChallengeIconView } from '../../../core/challenges/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '../../../core/challenges/challenge-visual.constants';
import { ChallengesApi } from '../../../core/challenges/challenges-api';
import { TranslatePipe } from '../../../core/i18n/translate-pipe';
import { ProgressBar } from '../../../shared/progress-bar/progress-bar';
import { ResourceState } from '../../../shared/resource-state/resource-state';
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
    MatTooltip,
    ProgressBar,
    ResourceState,
    LucideChevronDown,
    LucideTarget,
    LucideTrophy,
  ],
  templateUrl: './weekly-challenges.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
   * Whether the card's content is currently visible, toggled by the accordion header.
   */
  protected readonly isExpanded = signal(true);

  /**
   * Challenges of the active week, paired with their resolved icon and color treatment.
   */
  protected readonly rows = computed<readonly ChallengeRow[]>(
    () =>
      this.challengesResource.value()?.challenges.map((challenge) => ({
        id: challenge.id,
        name: challenge.name,
        description: challenge.description,
        completedPlayers: challenge.completedPlayers,
        totalPlayers: challenge.totalPlayers,
        completionPercentage: challenge.completionPercentage,
        points: challenge.points,
        visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
      })) ?? [],
  );

  /**
   * Toggles the card's content between expanded and collapsed.
   */
  protected toggleExpanded(): void {
    this.isExpanded.update((expanded) => !expanded);
  }
}
