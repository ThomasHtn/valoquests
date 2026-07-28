import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { LucideChevronDown, LucideTrophy, LucideUser } from '@lucide/angular';

import { ChallengeIconView } from '../../../core/challenges/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '../../../core/challenges/challenge-visual.constants';
import { ChallengesApi } from '../../../core/challenges/challenges-api';
import { TranslatePipe } from '../../../core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '../../../core/players/player-avatar';
import { RankingApi } from '../../../core/ranking/ranking-api';
import { resolvePositionBadgeClass } from './weekly-ranking.constants';
import { RankingCell, RankingColumn, RankingRow } from './weekly-ranking.model';
import { buildValueLabel, computeCompletionPercentage } from './weekly-ranking.utils';

/**
 * "Weekly ranking" card of the overview page.
 *
 * Displays every tracked player's position, score and exact progress toward each challenge
 * selected for the active week, reusing the challenge color language from the weekly challenges
 * card so both widgets read as one system.
 */
@Component({
  selector: 'app-weekly-ranking',
  imports: [
    TranslatePipe,
    ChallengeIconView,
    MatTooltip,
    LucideChevronDown,
    LucideTrophy,
    LucideUser,
  ],
  templateUrl: './weekly-ranking.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeeklyRanking {
  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Data-access service backing the shared current-challenges resource, used to resolve each
   * column's icon and color treatment from its difficulty tier.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Reactive resource fetching the current week's ranking.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header and
   * the weekly challenges card.
   */
  private readonly challengesResource = this.challengesApi.current;

  /**
   * Whether the card's content is currently visible, toggled by the accordion header.
   */
  protected readonly isExpanded = signal(true);

  /**
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = computed(
    () => this.rankingResource.isLoading() || this.challengesResource.isLoading(),
  );

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = computed(
    () => !!this.rankingResource.error() || !!this.challengesResource.error(),
  );

  /**
   * Challenges selected for the active week, paired with their resolved icon and color treatment,
   * used both as table columns and to resolve each row's per-challenge cell visual.
   */
  protected readonly columns = computed<readonly RankingColumn[]>(
    () =>
      this.challengesResource.value()?.challenges.map((challenge) => ({
        challengeId: challenge.id,
        name: challenge.name,
        tooltip: `${challenge.name} — ${challenge.description}`,
        visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
      })) ?? [],
  );

  /**
   * Ranking entries mapped to display-ready rows: one cell per column, aligned by challenge id.
   */
  protected readonly rows = computed<readonly RankingRow[]>(() => {
    const columns = this.columns();
    return (
      this.rankingResource.value()?.ranking.map((entry) => {
        const cells: RankingCell[] = columns.map((column) => {
          const progress = entry.challengeProgress.find(
            (candidate) => candidate.challengeId === column.challengeId,
          );
          return {
            challengeId: column.challengeId,
            valueLabel: buildValueLabel(progress),
            completionPercentage: computeCompletionPercentage(progress),
            visual: column.visual,
          };
        });

        return {
          position: entry.position,
          playerId: entry.player.id,
          displayName: entry.player.displayName,
          avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
          points: entry.points,
          cells,
        };
      }) ?? []
    );
  });

  /**
   * Resolves the badge classes for a row's position, exposed to the template.
   */
  protected readonly positionBadgeClass = resolvePositionBadgeClass;

  /**
   * Toggles the card's content between expanded and collapsed.
   */
  protected toggleExpanded(): void {
    this.isExpanded.update((expanded) => !expanded);
  }
}
