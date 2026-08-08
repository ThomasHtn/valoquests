import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCrown } from '@lucide/angular';

import { formatDateRange, isoWeekNumber } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { Avatar } from '@shared/avatar/avatar';
import { ChallengeCompletionBadge } from '@shared/challenge-completion-badge/challenge-completion-badge';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { DamageBadge } from '@shared/damage-badge/damage-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { resolveRankingPodiumTier } from './ranking-podium.constants';
import { RankingHistoryWeekView } from './ranking.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Number of challenges selected for a week, i.e. the denominator {@link ChallengeCompletionBadge}
 * renders for every row: one per difficulty, fixed by `ChallengeDifficulty` on the backend (see
 * the root CLAUDE.md). `RankingHistoryEntry` has no `totalChallenges` field of its own to read
 * this from — unlike the current week's `RankingEntry` — since a finalized week's challenge count
 * never varies.
 */
const WEEKLY_CHALLENGE_COUNT = 5;

/**
 * Ranking history page.
 *
 * Browses the finalized ranking of every completed calendar week as a carousel, one week at a
 * time, from the most recent to the oldest, jumped to directly through the quick-access
 * dropdown.
 */
@Component({
  selector: 'app-ranking',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    ChallengeCompletionBadge,
    ChampionBadge,
    DamageBadge,
    PositionBadge,
    ResourceState,
    Select,
    LucideCrown,
  ],
  templateUrl: './ranking.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Ranking {
  /**
   * Data-access service backing the ranking history resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Data-access service backing the shared players resource, used to resolve each row's avatar.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to resolve the translated week label for each historical week.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary, used to resolve avatars.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Reactive resource fetching every finalized week of ranking history in one request.
   */
  protected readonly historyResource = this.rankingApi.history;

  /**
   * Zero-based index of the week currently displayed by the carousel, 0 being the most recent
   * finalized week.
   */
  protected readonly weekIndex = signal(0);

  /**
   * Direction of the most recent navigation step, driving which side the incoming week's entrance
   * transition slides in from: `'previous'` (a more recent week) slides in from the left,
   * `'next'` (an older week) slides in from the right — mirroring the jumped-to week's position
   * relative to the one it replaces.
   */
  protected readonly navigationDirection = signal<'previous' | 'next'>('next');

  /**
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.historyResource, this.playersResource);

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = anyError(this.historyResource, this.playersResource);

  /**
   * Avatar URL per player id, resolved from the shared players resource.
   */
  private readonly avatarUrlByPlayerId = computed(
    () =>
      new Map(
        resourceValue(this.playersResource, []).map(
          (player) => [player.id, resolvePlayerAvatarUrl(player.portrait)] as const,
        ),
      ),
  );

  /**
   * Every finalized week, mapped to display-ready blocks, most recent first.
   */
  protected readonly weeks = computed<readonly RankingHistoryWeekView[]>(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();

    return (resourceValue(this.historyResource, null)?.content ?? []).map((week) => {
      const rows = week.ranking.map((entry) => ({
        position: entry.position,
        playerId: entry.playerId,
        displayName: entry.displayName,
        avatarUrl: avatarUrlByPlayerId.get(entry.playerId) ?? null,
        damage: entry.challengeDamage,
        completedChallenges: entry.completedChallenges,
      }));

      return {
        weekStart: week.weekStart,
        weekLabel: this.translation.translate('ranking.week.label', {
          number: isoWeekNumber(week.weekStart),
        }),
        dateRangeLabel: formatDateRange(week.weekStart, week.weekEnd),
        top3: rows.slice(0, 3),
        rest: rows.slice(3),
      };
    });
  });

  /**
   * Total number of finalized weeks available to browse.
   */
  protected readonly totalWeeks = computed(() => this.weeks().length);

  /**
   * The week currently displayed by the carousel, or `null` before data has loaded or once the
   * index falls outside the available weeks (e.g. right after a reload).
   */
  protected readonly currentWeek = computed<RankingHistoryWeekView | null>(
    () => this.weeks()[this.weekIndex()] ?? null,
  );

  /**
   * {@link currentWeek} wrapped in a single-item array, keyed by its `weekStart` in the template's
   * `@for`. Re-keying rather than a plain `@if` forces Angular to recreate the week's DOM node on
   * every navigation step, which replays its CSS entrance animation — the carousel's one authored
   * motion moment — instead of only playing once on first load.
   */
  protected readonly currentWeekSlide = computed<readonly RankingHistoryWeekView[]>(() => {
    const week = this.currentWeek();
    return week ? [week] : [];
  });

  /**
   * CSS `translateX` starting offset for the current week's entrance transition, read by the
   * template through the `--week-enter-offset` custom property (see `week-enter` in styles.css).
   * Negative slides in from the left, positive from the right.
   */
  protected readonly weekEnterOffset = computed(() =>
    this.navigationDirection() === 'previous' ? '-16px' : '16px',
  );

  /**
   * One option per finalized week, offered by the quick-jump dropdown, most recent first.
   */
  protected readonly weekOptions = computed<readonly SelectOption<number>[]>(() =>
    this.weeks().map((week, index) => ({
      value: index,
      label: `${week.weekLabel} · ${week.dateRangeLabel}`,
    })),
  );

  /**
   * Placeholder line width driving the loading skeleton.
   */
  protected readonly skeletonWidth = SKELETON_ROWS[0];

  /**
   * Number of challenges selected for a week, exposed to the template as
   * {@link ChallengeCompletionBadge}'s denominator.
   */
  protected readonly weeklyChallengeCount = WEEKLY_CHALLENGE_COUNT;

  /**
   * Resolves a podium row's visual tier (card tint, plinth, numeral) from its position, exposed
   * to the template.
   */
  protected readonly podiumTier = resolveRankingPodiumTier;

  /**
   * Reloads both backing resources after a failure.
   *
   * Both are retried because {@link hasError} reports their combined state and cannot tell which
   * one failed.
   */
  protected reload(): void {
    reloadAll(this.historyResource, this.playersResource);
  }

  /**
   * Jumps the carousel straight to the week chosen from the quick-jump dropdown.
   *
   * @param index - The selected week's zero-based index, or `null` when the dropdown is cleared
   * (never emitted in practice since every option always maps to a valid week).
   */
  protected onWeekSelect(index: number | null): void {
    if (index !== null) {
      this.navigationDirection.set(index < this.weekIndex() ? 'previous' : 'next');
      this.weekIndex.set(index);
    }
  }
}
