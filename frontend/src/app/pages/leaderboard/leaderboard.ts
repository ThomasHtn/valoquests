import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCalendar,
  LucideChevronLeft,
  LucideChevronRight,
  LucideFlame,
  LucideSwords,
  LucideTarget,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import {
  RankingChallengeProgress,
  RankingEntry,
  RankingHistoryEntry,
  RankingHistoryWeek,
} from '@core/ranking/ranking.model';
import { PageHeader } from '@layout/page-header/page-header';
import { Avatar } from '@shared/avatar/avatar';
import { ChallengeRing } from '@shared/challenge-ring/challenge-ring';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Tooltip } from '@shared/tooltip/tooltip';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { BoardRing, BoardRow, BoardTitle, BoardWeek } from './leaderboard.model';

const WEEK_DAYS = 7;

/**
 * Parses an ISO date (`YYYY-MM-DD`) as local midnight.
 */
function localMidnight(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/**
 * The week's ranking: who stands where, on what, and how far each operator is on every challenge
 * of the board. Closed weeks are browsed back to from the same page, frozen as they ended.
 *
 * The board is the backend's own order; nothing is re-sorted here.
 */
@Component({
  selector: 'app-leaderboard',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    PageHeader,
    ResourceState,
    Avatar,
    ChampionBadge,
    ChallengeRing,
    Tooltip,
    LucideCalendar,
    LucideChevronLeft,
    LucideChevronRight,
    LucideFlame,
    LucideSwords,
    LucideTarget,
    LucideWheat,
    LucideWrench,
    LucideZap,
  ],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Leaderboard {
  private readonly rankingApi = inject(RankingApi);

  private readonly campaignApi = inject(CampaignApi);

  private readonly playersApi = inject(PlayersApi);

  private readonly translation = inject(Translation);

  protected readonly currentResource = this.rankingApi.current;

  protected readonly historyResource = this.rankingApi.history;

  private readonly campaignResource = this.campaignApi.campaign;

  protected readonly isLoading = anyLoading(this.currentResource, this.campaignResource);

  protected readonly isError = anyError(this.currentResource);

  private readonly current = computed(() => resourceValue(this.currentResource, null) ?? null);

  private readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * Every closed week, newest first. Empty while it loads: the live week never waits for it.
   */
  private readonly history = computed<readonly RankingHistoryWeek[]>(
    () => resourceValue(this.historyResource, null)?.content ?? [],
  );

  /**
   * Portraits by operator: a closed week names its operators but carries no portrait.
   */
  private readonly portraits = computed(() => {
    const byId = new Map<number, string | null>();
    for (const player of resourceValue(this.playersApi.players, [])) {
      byId.set(player.id, player.portrait);
    }
    return byId;
  });

  /**
   * Mondays the page can show, newest first: the live week, then every closed one.
   */
  private readonly weekStarts = computed<readonly string[]>(() => {
    const live = this.current()?.weekStart;
    return [...(live ? [live] : []), ...this.history().map((week) => week.weekStart)];
  });

  /**
   * The Monday on screen: the live week by default, stepped back with the arrows.
   */
  protected readonly selectedWeekStart = linkedSignal<string | null>(
    () => this.weekStarts()[0] ?? null,
  );

  private readonly selectedIndex = computed(() =>
    this.weekStarts().indexOf(this.selectedWeekStart() ?? ''),
  );

  protected readonly canGoBack = computed(
    () => this.selectedIndex() >= 0 && this.selectedIndex() < this.weekStarts().length - 1,
  );

  protected readonly canGoForward = computed(() => this.selectedIndex() > 0);

  /**
   * Whoever finished first on the last closed week: the reigning champion, decorated on the live
   * board. A closed week decorates its own winner instead.
   */
  private readonly championId = computed(() => this.history()[0]?.winnerPlayerId ?? null);

  protected readonly board = computed<BoardWeek | null>(() => {
    const weekStart = this.selectedWeekStart();
    const current = this.current();
    if (weekStart === null) {
      return null;
    }
    if (current && current.weekStart === weekStart) {
      return this.liveBoard(current.ranking, weekStart);
    }
    const closed = this.history().find((week) => week.weekStart === weekStart);
    return closed ? this.closedBoard(closed) : null;
  });

  /**
   * The week, then the campaign's tier when the week belongs to one.
   */
  protected readonly headerEyebrow = computed(() => {
    const board = this.board();
    const campaign = this.campaign();
    if (!board) {
      return this.translation.translate('leaderboard.title');
    }
    const week =
      board.weekIndex !== null
        ? this.translation.translate('leaderboard.header.week', {
            week: board.weekIndex,
            weeks: CAMPAIGN_WEEK_COUNT,
          })
        : this.translation.translate('leaderboard.header.weekOf', {
            date: this.dayMonth(board.weekStart),
          });
    const tier =
      board.weekIndex !== null && campaign?.tier
        ? this.translation.translate('leaderboard.header.tier', {
            tier: this.translation.translate(`common.tier.${campaign.tier}`),
          })
        : '';
    return tier ? `${week} · ${tier}` : week;
  });

  /**
   * Beside the title: the day for the live week, the day the closed one was frozen otherwise.
   */
  protected readonly headingAside = computed(() => {
    const board = this.board();
    const current = this.current();
    if (!board) {
      return '';
    }
    if (board.live && current) {
      const weekday = this.weekday(current.today);
      return this.translation.translate('leaderboard.header.day', {
        weekday: weekday.charAt(0).toUpperCase() + weekday.slice(1),
        day: this.dayIndex(current.weekStart, current.today) + 1,
        days: WEEK_DAYS,
      });
    }
    const closed = this.history().find((week) => week.weekStart === board.weekStart);
    return closed
      ? this.translation.translate('leaderboard.header.closed', {
          date: this.dayMonth(closed.finalizedAt.slice(0, 10)),
        })
      : '';
  });

  /**
   * The strip's own label: the Monday, and whether the week is still running.
   */
  protected readonly weekLabel = computed(() => {
    const board = this.board();
    return board
      ? this.translation.translate('leaderboard.nav.weekOf', {
          date: this.weekSpan(board.weekStart),
        })
      : '';
  });

  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  protected readonly weekDays = WEEK_DAYS;

  protected retry(): void {
    reloadAll(this.currentResource, this.campaignResource);
  }

  protected goBack(): void {
    if (this.canGoBack()) {
      this.selectedWeekStart.set(this.weekStarts()[this.selectedIndex() + 1]);
    }
  }

  protected goForward(): void {
    if (this.canGoForward()) {
      this.selectedWeekStart.set(this.weekStarts()[this.selectedIndex() - 1]);
    }
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  private liveBoard(entries: readonly RankingEntry[], weekStart: string): BoardWeek {
    const champion = this.championId();
    const rows = entries.map((entry): BoardRow => ({
      playerId: entry.player.id,
      name: entry.player.displayName,
      portrait: resolvePlayerAvatarUrl(entry.player.portrait),
      position: entry.position,
      variation: entry.positionVariation,
      isChampion: entry.player.id === champion,
      total: entry.totalPoints,
      damage: entry.guardianDamage,
      challengePoints: entry.challengePoints,
      completedChallenges: entry.completedChallenges,
      totalChallenges: entry.totalChallenges,
      completedDaily: entry.completedDailyChallenges,
      streakDays: entry.streakDays,
      activeDays: entry.activeDays,
      titles: this.titles(entry.titles),
      rings: entry.challengeProgress.map((progress) => this.ring(progress)),
    }));
    return this.split(rows, weekStart, true);
  }

  private closedBoard(week: RankingHistoryWeek): BoardWeek {
    const portraits = this.portraits();
    const rows = week.ranking.map((entry: RankingHistoryEntry): BoardRow => ({
      playerId: entry.playerId,
      name: entry.displayName,
      portrait: resolvePlayerAvatarUrl(portraits.get(entry.playerId) ?? null),
      position: entry.position,
      variation: 0,
      isChampion: entry.playerId === week.winnerPlayerId,
      total: entry.totalPoints,
      damage: entry.guardianDamage,
      challengePoints: entry.challengePoints,
      completedChallenges: entry.completedChallenges,
      totalChallenges: 0,
      completedDaily: entry.completedDailyChallenges,
      streakDays: entry.streakDays,
      activeDays: entry.activeDays,
      titles: this.titles(entry.titles),
      rings: null,
    }));
    return this.split(rows, week.weekStart, false);
  }

  private split(rows: readonly BoardRow[], weekStart: string, live: boolean): BoardWeek {
    const weeks = this.campaign()?.weeks ?? [];
    const index = weeks.findIndex((week) => week.weekStart === weekStart);
    return {
      weekStart,
      live,
      weekIndex: index >= 0 ? index + 1 : null,
      ranked: rows.filter((row) => row.position !== null),
      unranked: rows.filter((row) => row.position === null),
    };
  }

  private titles(keys: readonly BoardTitle['key'][]): BoardTitle[] {
    return keys.map((key) => ({ key, ...resolveTitleVisual(key) }));
  }

  private ring(progress: RankingChallengeProgress): BoardRing {
    const visual = resolveChallengeVisual(progress.metric, progress.difficulty);
    const current = this.value(progress.currentValue);
    const target = progress.targetValue === null ? null : this.value(progress.targetValue);
    const ratio =
      progress.targetValue !== null && progress.targetValue > 0
        ? Math.min(100, (progress.currentValue / progress.targetValue) * 100)
        : progress.completed
          ? 100
          : 0;
    const tipKey = progress.completed ? 'ringTipDone' : target ? 'ringTip' : 'ringTipOpen';
    return {
      id: progress.id,
      cadence: progress.cadence,
      mark: visual.tier,
      categoryLabel: resolveChallengeMetricLabel(progress.metric, (key) =>
        this.translation.translate(key),
      ),
      currentValueLabel: current,
      compactValueLabel: new Intl.NumberFormat(this.locale(), {
        notation: 'compact',
        maximumFractionDigits: 1,
      }).format(progress.currentValue),
      targetValueLabel: target,
      completionPercentage: ratio,
      completed: progress.completed,
      visual: { iconClass: visual.iconClass, badgeClass: visual.badgeClass },
      tip: this.translation.translate(`leaderboard.board.${tipKey}`, {
        name: progress.name,
        current,
        target: target ?? '',
        points: progress.rankingPoints,
      }),
    };
  }

  private value(amount: number): string {
    return new Intl.NumberFormat(this.locale(), { maximumFractionDigits: 2 }).format(amount);
  }

  private dayIndex(weekStart: string, today: string): number {
    const offset = Math.round(
      (localMidnight(today).getTime() - localMidnight(weekStart).getTime()) / 86_400_000,
    );
    return Math.min(WEEK_DAYS - 1, Math.max(0, offset));
  }

  private locale(): string {
    return this.translation.language() === 'fr' ? 'fr-FR' : 'en-US';
  }

  private weekday(isoDate: string): string {
    return new Intl.DateTimeFormat(this.locale(), { weekday: 'long' }).format(
      localMidnight(isoDate),
    );
  }

  private dayMonth(isoDate: string): string {
    return new Intl.DateTimeFormat(this.locale(), { day: 'numeric', month: 'long' }).format(
      localMidnight(isoDate),
    );
  }

  /**
   * Monday to Sunday, the month spelled once when both days share it (`31 août – 6 sept.`).
   */
  private weekSpan(weekStart: string): string {
    const monday = localMidnight(weekStart);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + WEEK_DAYS - 1);
    const short = new Intl.DateTimeFormat(this.locale(), { day: 'numeric', month: 'short' });
    return `${short.format(monday)} – ${short.format(sunday)}`;
  }
}
