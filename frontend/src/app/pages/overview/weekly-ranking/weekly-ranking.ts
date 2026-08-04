import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Tooltip } from '@shared/tooltip/tooltip';
import { RouterLink } from '@angular/router';
import { LucideCheck, LucideChevronDown, LucideChevronUp, LucideTrophy } from '@lucide/angular';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PointsBadge } from '@shared/points-badge/points-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { RankingCell, RankingColumn, RankingRow } from './weekly-ranking.model';
import {
  buildCurrentValueLabel,
  buildTargetValueLabel,
  computeCompletionPercentage,
  formatMetricValue,
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
    NgTemplateOutlet,
    RouterLink,
    ChallengeIconView,
    Tooltip,
    Avatar,
    ChampionBadge,
    PointsBadge,
    PositionBadge,
    ProgressBar,
    ProgressCircle,
    ResourceState,
    LucideCheck,
    LucideChevronDown,
    LucideChevronUp,
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
   * i18n service used to resolve each challenge's translated category label.
   */
  private readonly translation = inject(Translation);

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
      categoryLabel: resolveChallengeMetricLabel(challenge.metric, (key) =>
        this.translation.translate(key),
      ),
      targetLabel: challenge.targetValue ? formatMetricValue(challenge.targetValue) : null,
      tooltip: `${challenge.name} — ${challenge.description}`,
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    })),
  );

  /**
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Ranking entries mapped to display-ready rows: one cell per column, aligned by challenge id.
   */
  protected readonly rows = computed<readonly RankingRow[]>(() => {
    const columns = this.columns();
    const championPlayerId = this.championPlayerId();
    return (resourceValue(this.rankingResource, null)?.ranking ?? []).map((entry) => {
      const cells: RankingCell[] = columns.map((column) => {
        const progress = entry.challengeProgress.find(
          (candidate) => candidate.challengeId === column.challengeId,
        );
        return {
          challengeId: column.challengeId,
          categoryLabel: column.categoryLabel,
          currentValueLabel: buildCurrentValueLabel(progress),
          targetValueLabel: buildTargetValueLabel(progress),
          completionPercentage: computeCompletionPercentage(progress),
          completed: progress?.completed ?? false,
          visual: column.visual,
        };
      });

      return {
        position: entry.position,
        positionVariation: entry.positionVariation,
        playerId: entry.player.id,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        points: entry.points,
        cells,
        isChampion: entry.player.id === championPlayerId,
      };
    });
  });

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Reloads both backing resources after a failure.
   *
   * Both are retried because {@link hasError} reports their combined state and cannot tell which
   * one failed.
   */
  protected reload(): void {
    reloadAll(this.rankingResource, this.challengesResource);
  }
}
