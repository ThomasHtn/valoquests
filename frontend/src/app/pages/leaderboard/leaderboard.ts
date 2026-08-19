import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageHeader } from '@layout/page-header/page-header';
import { Tooltip } from '@shared/tooltip/tooltip';
import { RouterLink } from '@angular/router';
import { LucideCheck, LucideChevronDown, LucideChevronUp } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { isoWeekNumber, RemainingTime, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { RankingCell, RankingColumn, RankingRow } from './leaderboard.model';
import {
  buildCurrentValueLabel,
  buildTargetValueLabel,
  computeCompletionPercentage,
  formatMetricValue,
} from './leaderboard.utils';

/**
 * Weekly leaderboard page — the squad's tactical matrix.
 *
 * Crosses the seven tracked players with the five challenges drawn for the active week: one row
 * per player, one column per challenge, so who is carrying the week and which challenge the squad
 * is collectively stuck on are both readable in one glance. Reuses the challenge color language
 * from the quest page, so a tier means the same thing on both screens. Distinct from `/ranking`,
 * which browses the finalized history of past weeks rather than the live current one.
 */
@Component({
  selector: 'app-leaderboard',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    ChallengeIconView,
    Tooltip,
    Avatar,
    ChampionBadge,
    PositionBadge,
    ProgressBar,
    ProgressCircle,
    ResourceState,
    WeekCountdown,
    LucideCheck,
    LucideChevronDown,
    LucideChevronUp,
    PageHeader,
  ],
  templateUrl: './leaderboard.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Leaderboard {
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
   * i18n service used to resolve each challenge's translated category label, and read for the
   * active language when grouping damage amounts.
   */
  private readonly translation = inject(Translation);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Reactive resource fetching the current week's ranking.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview hero and
   * the challenges page.
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
   * The active week as the ranking describes it, or `null` until it has loaded.
   */
  private readonly currentWeek = computed(() => resourceValue(this.rankingResource, null) ?? null);

  /**
   * Active week's ISO number, or `null` while loading.
   */
  protected readonly weekNumber = computed<number | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : isoWeekNumber(currentWeek.weekStart);
  });

  /**
   * Time left before the weekly rollover, or `null` while loading. Same countdown as the overview
   * and quest pages, since it is the deadline all three screens are counting down to.
   */
  protected readonly remaining = computed<RemainingTime | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : remainingWeekTime(currentWeek.weekEnd, this.now());
  });

  /**
   * Challenges selected for the active week, paired with their resolved icon and color treatment,
   * used both as table columns and to resolve each row's per-challenge cell visual.
   */
  protected readonly columns = computed<readonly RankingColumn[]>(() =>
    (resourceValue(this.challengesResource, null)?.challenges ?? []).map((challenge) => ({
      challengeId: challenge.id,
      name: challenge.name,
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
    const language = this.translation.language();
    return (this.currentWeek()?.ranking ?? []).map((entry) => {
      const cells: RankingCell[] = columns.map((column) => {
        const progress = entry.challengeProgress.find(
          (candidate) => candidate.challengeId === column.challengeId,
        );
        return {
          challengeId: column.challengeId,
          name: column.name,
          categoryLabel: column.categoryLabel,
          currentValueLabel: buildCurrentValueLabel(progress),
          targetValueLabel: buildTargetValueLabel(progress),
          completionPercentage: computeCompletionPercentage(progress),
          completed: progress?.completed ?? false,
          visual: column.visual,
        };
      });

      const bonus = entry.regularityBonus + entry.teamBonus;

      return {
        // The backend omits `position` entirely from the JSON payload when null (global
        // non-null serialization), so the parsed value is `undefined`, not `null` - normalized
        // here so every `=== null` check downstream (component and template) is reliable.
        position: entry.position ?? null,
        positionVariation: entry.positionVariation,
        playerId: entry.player.id,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        // `totalDamage`, not `challengeDamage`: the ranking is ordered on the total, so showing
        // anything else next to a position would not explain the order it is in.
        damageLabel: formatDamage(entry.totalDamage, language),
        bonusLabel: bonus === 0 ? null : `+${formatDamage(bonus, language)}`,
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
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }

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
