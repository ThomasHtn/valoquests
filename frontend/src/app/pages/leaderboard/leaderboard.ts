import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronDown, LucideChevronUp, LucideDynamicIcon } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { weekDayIndex } from '@core/campaign/calendar/campaign-calendar.utils';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { CampaignHistory } from '@core/campaign/campaign-history.model';
import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { primaryTitle } from '@core/campaign/titles/campaign-title.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { CHALLENGE_DIFFICULTIES } from '@core/challenges/challenge.constants';
import { WEEK_DAYS } from '@core/date/date.constants';
import { daysBetween } from '@core/date/date.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolveLocale } from '@core/i18n/format/locale.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TranslateFn } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import {
  DailyRankingEntry,
  RankingEntry,
  RankingHistoryEntry,
  RankingHistoryWeek,
} from '@core/ranking/ranking.model';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import {
  EmptyPlate as EmptyPlateContent,
  EmptyReadout,
} from '@shared/empty-plate/empty-plate.model';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { StreakGauge } from '@shared/streak-gauge/streak-gauge';
import { streakBonusOf, streakWeekOf } from '@shared/streak-gauge/streak-gauge.utils';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';
import {
  boardColumns,
  formatWeekSpan,
  placeWeekInCampaign,
  resolveSelectedWeek,
  resolveTitleMeasures,
  weekChallengeCeiling,
} from './leaderboard-board.utils';
import { BoardRow, BoardStreak, BoardTitle, BoardWeek, WeekOption } from './leaderboard.model';
import { Podium } from './podium/podium';
import { WeekPicker } from './week-picker/week-picker';

/**
 * Weekly ranking, live or a closed week browsed back to.
 * Rows keep the backend's order; nothing is re-sorted here.
 */
@Component({
  selector: 'app-leaderboard',
  imports: [
    LucideDynamicIcon,
    LucideChevronDown,
    LucideChevronUp,
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    PageHeader,
    EmptyPlate,
    ResourceState,
    StreakGauge,
    Avatar,
    ChampionBadge,
    PositionBadge,
    Podium,
    TitleBadge,
    Tooltip,
    WeekPicker,
  ],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Leaderboard {
  /**
   * Ranking feed: live week, closed weeks and today's board.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Campaign feed, to place each week in its campaign.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Players feed, for the portraits closed weeks lack.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Translation service, for the labels built in code.
   */
  private readonly translation = inject(Translation);

  /**
   * The live week's ranking.
   */
  private readonly currentResource = this.rankingApi.current;

  /**
   * Closed weeks' rankings, offered in the week picker.
   */
  private readonly historyResource = this.rankingApi.history;

  /**
   * The running campaign, for its weeks and difficulty.
   */
  private readonly campaignResource = this.campaignApi.campaign;

  /**
   * Closed campaigns, to place their weeks.
   */
  private readonly campaignHistoryResource = this.campaignApi.history;

  /**
   * Whether the live ranking or the campaign is still loading.
   */
  protected readonly isLoading = anyLoading(this.currentResource, this.campaignResource);

  /**
   * Whether the live ranking or the campaign failed to load.
   */
  protected readonly isError = anyError(this.currentResource, this.campaignResource);

  /**
   * The live week's ranking, or `null` until loaded.
   */
  private readonly current = computed(() => resourceValue(this.currentResource, null) ?? null);

  /**
   * The running campaign, or `null` until loaded.
   */
  private readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * Closed campaigns, to place their weeks after they ended.
   */
  private readonly campaignHistory = computed<readonly CampaignHistory[]>(() =>
    resourceValue(this.campaignHistoryResource, []),
  );

  /**
   * Closed weeks, newest first; empty while loading so the live week never waits.
   */
  private readonly history = computed<readonly RankingHistoryWeek[]>(
    () => resourceValue(this.historyResource, null)?.content ?? [],
  );

  /**
   * Today's board, the only one that knows the days each operator played.
   */
  private readonly daily = computed(() => resourceValue(this.rankingApi.daily, null) ?? null);

  /**
   * Portraits by player id: closed weeks carry none.
   */
  private readonly portraits = computed(() => {
    const byId = new Map<number, string | null>();
    for (const player of resourceValue(this.playersApi.players, [])) {
      byId.set(player.id, player.portrait);
    }
    return byId;
  });

  /**
   * Mondays on offer, newest first: the live week, then closed ones.
   */
  private readonly weekStarts = computed<readonly string[]>(() => {
    const live = this.current()?.weekStart;
    return [...(live ? [live] : []), ...this.history().map((week) => week.weekStart)];
  });

  /**
   * Monday on screen, the live week by default, kept across reloads.
   */
  protected readonly selectedWeekStart = linkedSignal<readonly string[], string | null>({
    source: () => this.weekStarts(),
    computation: (weekStarts, previous) => resolveSelectedWeek(weekStarts, previous),
  });

  /**
   * Last closed week's winner, decorated as champion on the live board.
   */
  private readonly championId = computed(() => this.history()[0]?.winnerPlayerId ?? null);

  /**
   * The selected week's board, live or closed; `null` while nothing is loaded.
   */
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
   * Whether anyone scored: an all-zero week shows no podium.
   */
  protected readonly hasActivity = computed(
    () => this.board()?.ranked.some((row) => row.total > 0) ?? false,
  );

  /**
   * Picker weeks, newest first, placed in their campaign.
   */
  protected readonly weekOptions = computed<readonly WeekOption[]>(() => {
    const current = this.current();
    const portraits = this.portraits();
    const closed = this.history().map((week): WeekOption => {
      const winner = week.ranking.find((entry) => entry.playerId === week.winnerPlayerId);
      return {
        weekStart: week.weekStart,
        label: this.weekSpan(week.weekStart),
        ...this.placeWeek(week.weekStart),
        live: false,
        winner: winner
          ? {
              name: winner.displayName,
              portrait: resolvePlayerAvatarUrl(portraits.get(winner.playerId) ?? null),
            }
          : null,
      };
    });
    if (!current) {
      return closed;
    }
    return [
      {
        weekStart: current.weekStart,
        label: this.weekSpan(current.weekStart),
        ...this.placeWeek(current.weekStart),
        live: true,
        winner: null,
      },
      ...closed,
    ];
  });

  /**
   * Whether the week belongs to a campaign (damage and wounded) rather than plain points.
   */
  private readonly rescueActive = computed(() => this.board()?.weekIndex != null);

  /**
   * Figure columns, named for the week's kind.
   */
  protected readonly columns = computed(() => boardColumns(this.rescueActive()));

  /**
   * Eyebrow: the week, then the campaign difficulty.
   */
  private readonly headerEyebrow = computed(() => {
    const board = this.board();
    const campaign = this.campaign();
    if (!board) {
      return this.translation.translate('leaderboard.title');
    }
    // Outside a campaign the picker already shows the dates.
    const week =
      board.weekIndex !== null
        ? this.translation.translate('leaderboard.header.week', {
            week: board.weekIndex,
            weeks: CAMPAIGN_WEEK_COUNT,
          })
        : this.translation.translate('leaderboard.header.outsideCampaign');
    const difficulty =
      board.weekIndex !== null && campaign?.difficulty
        ? this.translation.translate('leaderboard.header.difficulty', {
            difficulty: this.translation.translate(`common.difficulty.${campaign.difficulty}`),
          })
        : '';
    return difficulty ? `${week} · ${difficulty}` : week;
  });

  /**
   * Empty state before the week's first synchronization.
   */
  protected readonly emptyPlate = computed<EmptyPlateContent>(() => {
    const t = (suffix: string) => this.translation.translate(`leaderboard.state.empty.${suffix}`);
    return {
      illustration: 'podium',
      eyebrow: t('eyebrow'),
      title: t('title'),
      text: t('text'),
      readouts: [],
    };
  });

  /**
   * Empty state of a board with nobody ranked, with the day on a live week.
   */
  protected readonly nobodyPlate = computed<EmptyPlateContent>(() => {
    const board = this.board();
    const current = this.current();
    const t: TranslateFn = (suffix, params) =>
      this.translation.translate(`leaderboard.board.nobody.${suffix}`, params);
    const readouts: EmptyReadout[] = [];
    if (board?.live && current) {
      readouts.push({
        tone: 'info',
        label: t('day'),
        value: t('dayValue', {
          day: weekDayIndex(current.weekStart, current.today) + 1,
          days: WEEK_DAYS,
        }),
      });
      readouts.push({ tone: 'todo', label: t('reset'), value: t('resetValue') });
    }
    return {
      illustration: 'podium',
      eyebrow: this.headerEyebrow(),
      title: t('title'),
      text: t('text'),
      readouts,
    };
  });

  /**
   * Reloads the live ranking and the campaign after a failure.
   */
  protected retry(): void {
    reloadAll(this.currentResource, this.campaignResource);
  }

  /**
   * Shows the week picked in the picker.
   */
  protected selectWeek(weekStart: string): void {
    this.selectedWeekStart.set(weekStart);
  }

  /**
   * Formats a score in the reader's language.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }

  /**
   * Live week board, with titles measured and streaks read from today's board.
   */
  private liveBoard(entries: readonly RankingEntry[], weekStart: string): BoardWeek {
    const champion = this.championId();
    const daily = this.daily();
    // Yesterday's board still answers around the Monday rollover: ignore it.
    const offset = daily ? daysBetween(weekStart, daily.day) : -1;
    const today = daily && offset >= 0 && offset < WEEK_DAYS ? daily : null;
    const days = new Map(today?.ranking.map((line) => [line.playerId, line]) ?? []);
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
      title: this.title(entry.titles, resolveTitleMeasures(entry)),
      challengesCompleted: entry.completedChallenges + entry.completedDailyChallenges,
      challengesMax: weekChallengeCeiling(entry.totalChallenges),
      matchCount: entry.matchCount,
      streak: this.liveStreak(entry, today?.day ?? null, days.get(entry.player.id)),
    }));
    return this.split(rows, weekStart, true);
  }

  /**
   * The figure that earned a title, worded for its badge.
   */
  private measure(key: WeeklyTitle, value: number): string {
    return this.translation.translate(`leaderboard.board.measure.${key}`, {
      value: this.format(value),
    });
  }

  /**
   * Closed week board, rebuilt from the archived ranking.
   */
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
      title: this.title(entry.titles, {
        REGULAR: entry.playedDays,
        SCOUT: entry.completedChallenges + entry.completedDailyChallenges,
      }),
      challengesCompleted: entry.completedChallenges + entry.completedDailyChallenges,
      // A closed week keeps no draw size: one challenge per tier.
      challengesMax: weekChallengeCeiling(CHALLENGE_DIFFICULTIES.length),
      matchCount: entry.matchCount,
      streak: { week: null, days: entry.playedDays, bonusPercent: streakBonusOf(entry.playedDays) },
    }));
    return this.split(rows, week.weekStart, false);
  }

  /**
   * Live streak: days around today, and today's bonus or the one playing would earn.
   */
  private liveStreak(
    entry: RankingEntry,
    day: string | null,
    line: DailyRankingEntry | undefined,
  ): BoardStreak {
    if (day === null || !line) {
      return { week: null, days: entry.playedDays, bonusPercent: streakBonusOf(entry.playedDays) };
    }
    const days = line.weekPlayedDays.length;
    return {
      week: streakWeekOf(day, line.weekPlayedDays),
      days,
      bonusPercent: line.matchCount > 0 ? line.streakBonusPercent : streakBonusOf(days + 1),
    };
  }

  /**
   * Splits rows into ranked and unranked, placing the week in its campaign.
   */
  private split(rows: readonly BoardRow[], weekStart: string, live: boolean): BoardWeek {
    return {
      weekStart,
      live,
      weekIndex: this.placeWeek(weekStart).index,
      ranked: rows.filter((row) => row.position !== null),
      unranked: rows.filter((row) => row.position === null),
    };
  }

  /**
   * The week's index and campaign group, for the picker and the header.
   */
  private placeWeek(weekStart: string): Pick<WeekOption, 'index' | 'group'> {
    return placeWeekInCampaign(weekStart, this.campaign(), this.campaignHistory());
  }

  /**
   * Highest-priority title held, `null` when none.
   */
  private title(
    keys: readonly BoardTitle['key'][],
    measures: Partial<Record<WeeklyTitle, number>>,
  ): BoardTitle | null {
    const key = primaryTitle(keys);
    if (key === null) {
      return null;
    }
    const value = measures[key];
    return { key, measure: value === undefined ? null : this.measure(key, value) };
  }

  /**
   * Locale of the chosen language, for date formats.
   */
  private locale(): string {
    return resolveLocale(this.translation.language());
  }

  /**
   * The week's first and last days, as the picker labels it.
   */
  private weekSpan(weekStart: string): string {
    return formatWeekSpan(weekStart, this.locale());
  }
}
