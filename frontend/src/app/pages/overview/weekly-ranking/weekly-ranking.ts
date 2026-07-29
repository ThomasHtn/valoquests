import { Component, computed, inject } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { LucideTrophy } from '@lucide/angular';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { Avatar } from '@shared/avatar/avatar';
import { CollapsibleCard } from '@shared/collapsible-card/collapsible-card';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { RankingCell, RankingColumn, RankingRow } from './weekly-ranking.model';
import {
  buildCurrentValueLabel,
  buildTargetValueLabel,
  computeCompletionPercentage,
} from './weekly-ranking.utils';

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
    CollapsibleCard,
    MatTooltip,
    Avatar,
    ProgressBar,
    ResourceState,
    LucideTrophy,
  ],
  templateUrl: './weekly-ranking.html',
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
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.rankingResource, this.challengesResource);

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = anyError(this.rankingResource, this.challengesResource);

  /**
   * Challenges selected for the active week, paired with their resolved icon and color treatment,
   * used both as table columns and to resolve each row's per-challenge cell visual.
   */
  protected readonly columns = computed<readonly RankingColumn[]>(() =>
    (resourceValue(this.challengesResource, null)?.challenges ?? []).map((challenge) => ({
      challengeId: challenge.id,
      name: challenge.name,
      tooltip: `${challenge.name} — ${challenge.description}`,
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    })),
  );

  /**
   * Ranking entries mapped to display-ready rows: one cell per column, aligned by challenge id.
   */
  protected readonly rows = computed<readonly RankingRow[]>(() => {
    const columns = this.columns();
    return (resourceValue(this.rankingResource, null)?.ranking ?? []).map((entry) => {
      const cells: RankingCell[] = columns.map((column) => {
        const progress = entry.challengeProgress.find(
          (candidate) => candidate.challengeId === column.challengeId,
        );
        return {
          challengeId: column.challengeId,
          currentValueLabel: buildCurrentValueLabel(progress),
          targetValueLabel: buildTargetValueLabel(progress),
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
    });
  });

  /**
   * Resolves the badge classes for a row's position, exposed to the template.
   */
  protected readonly positionBadgeClass = resolvePositionBadgeClass;
}
