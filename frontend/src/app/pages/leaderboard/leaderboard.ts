import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCheck, LucideChevronDown, LucideChevronUp } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { CAMPAIGN_WEEK_COUNT, CampaignHistory, WeeklyTitle } from '@core/campaign/campaign.model';
import { primaryTitle } from '@core/campaign/campaign-title.utils';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { WEEK_DAYS } from '@core/date/date-time.constants';
import { daysBetween } from '@core/date/date-time.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolveLocale } from '@core/i18n/locale.utils';
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
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import {
  EmptyPlate as EmptyPlateContent,
  EmptyReadout,
} from '@shared/empty-plate/empty-plate.model';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import {
  buildBoardColumns,
  formatFigure,
  formatWeekSpan,
  placeWeekInCampaign,
  resolveTitleMeasures,
} from './leaderboard-board.utils';
import { BoardProgress, BoardRow, BoardTitle, BoardWeek, WeekOption } from './leaderboard.model';
import { Podium } from './podium/podium';
import { WeekPicker } from './week-picker/week-picker';

/**
 * The week's ranking: who stands where, on what, and how far each operator is on every weekly
 * challenge of the board. Closed weeks are browsed back to from the same page, frozen as they
 * ended.
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
    EmptyPlate,
    ResourceState,
    Avatar,
    ChallengeRing,
    ChampionBadge,
    PositionBadge,
    Podium,
    TitleBadge,
    ProgressBar,
    Tooltip,
    WeekPicker,
    LucideCheck,
    LucideChevronDown,
    LucideChevronUp,
  ],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Leaderboard {
  private readonly rankingApi = inject(RankingApi);

  private readonly campaignApi = inject(CampaignApi);

  private readonly challengesApi = inject(ChallengesApi);

  private readonly playersApi = inject(PlayersApi);

  private readonly translation = inject(Translation);

  protected readonly currentResource = this.rankingApi.current;

  protected readonly historyResource = this.rankingApi.history;

  private readonly campaignResource = this.campaignApi.campaign;

  private readonly campaignHistoryResource = this.campaignApi.history;

  protected readonly isLoading = anyLoading(this.currentResource, this.campaignResource);

  protected readonly isError = anyError(this.currentResource);

  private readonly current = computed(() => resourceValue(this.currentResource, null) ?? null);

  private readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * Every closed campaign, so a week of one can still be placed in it after it ended.
   */
  private readonly campaignHistory = computed<readonly CampaignHistory[]>(() =>
    resourceValue(this.campaignHistoryResource, []),
  );

  /**
   * Every closed week, newest first. Empty while it loads: the live week never waits for it.
   */
  private readonly history = computed<readonly RankingHistoryWeek[]>(
    () => resourceValue(this.historyResource, null)?.content ?? [],
  );

  /**
   * What each challenge of the week asks for, by id: the ranking carries a challenge's name and
   * figures but not its description, which the week's own draw does.
   */
  private readonly descriptions = computed(() => {
    const byId = new Map<number, string>();
    for (const challenge of resourceValue(this.challengesApi.current, null)?.challenges ?? []) {
      byId.set(challenge.id, challenge.description);
    }
    return byId;
  });

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
   * The Monday on screen: the live week by default, changed from the picker.
   */
  protected readonly selectedWeekStart = linkedSignal<string | null>(
    () => this.weekStarts()[0] ?? null,
  );

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
   * A week nobody has played yet still ranks the whole field, all on zero. The podium would crown
   * three names on nothing, so it waits for the first figure.
   */
  protected readonly hasActivity = computed(
    () => this.board()?.ranked.some((row) => row.total > 0) ?? false,
  );

  /**
   * Every week the picker offers, newest first, each placed in its campaign when it has one.
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
   * Whether the week on screen belongs to a campaign: its figures are then guardian damage and
   * wounded brought home, otherwise ranking points with no guardian nor base behind them.
   */
  protected readonly rescueActive = computed(() => this.board()?.weekIndex != null);

  /**
   * The week, then the campaign's difficulty when the week belongs to one.
   */
  protected readonly headerEyebrow = computed(() => {
    const board = this.board();
    const campaign = this.campaign();
    if (!board) {
      return this.translation.translate('leaderboard.title');
    }
    // Outside a campaign the picker beside the bar already names the week's dates; the eyebrow
    // then states the one thing the picker does not, and stays short enough for a phone.
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
   * The empty state when no board exists yet: the first synchronization of the week has not run.
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
   * The empty state inside a board nobody is ranked in: the day for a live week, nothing more for
   * a closed one — it ended that way.
   */
  protected readonly nobodyPlate = computed<EmptyPlateContent>(() => {
    const board = this.board();
    const current = this.current();
    const t = (suffix: string, params?: Readonly<Record<string, number>>) =>
      this.translation.translate(`leaderboard.board.nobody.${suffix}`, params);
    const readouts: EmptyReadout[] = [];
    if (board?.live && current) {
      readouts.push({
        tone: 'info',
        label: t('day'),
        value: t('dayValue', {
          day:
            Math.min(WEEK_DAYS - 1, Math.max(0, daysBetween(current.weekStart, current.today))) + 1,
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

  protected retry(): void {
    reloadAll(this.currentResource, this.campaignResource);
  }

  protected selectWeek(weekStart: string): void {
    this.selectedWeekStart.set(weekStart);
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
      title: this.title(entry.titles, resolveTitleMeasures(entry)),
      // The day's challenge first: it changes every morning, so its column is the one that moves.
      progress: [...entry.challengeProgress]
        .sort((a, b) => Number(b.cadence === 'DAILY') - Number(a.cadence === 'DAILY'))
        .map((progress) => this.progress(progress)),
    }));
    return this.split(rows, weekStart, true);
  }

  private measure(key: WeeklyTitle, value: number): string {
    return this.translation.translate(`leaderboard.board.measure.${key}`, {
      value: this.format(value),
    });
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
      title: this.title(entry.titles, {
        REGULAR: entry.streakDays,
        SCOUT: entry.completedChallenges + entry.completedDailyChallenges,
      }),
      progress: null,
    }));
    return this.split(rows, week.weekStart, false);
  }

  private split(rows: readonly BoardRow[], weekStart: string, live: boolean): BoardWeek {
    return {
      weekStart,
      live,
      weekIndex: this.placeWeek(weekStart).index,
      columns: buildBoardColumns(rows),
      ranked: rows.filter((row) => row.position !== null),
      unranked: rows.filter((row) => row.position === null),
    };
  }

  private placeWeek(weekStart: string): Pick<WeekOption, 'index' | 'group'> {
    return placeWeekInCampaign(weekStart, this.campaign(), this.campaignHistory());
  }

  /**
   * The single title an operator is decorated with: the highest-priority one they hold, or `null`
   * when they hold none.
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
    return {
      key,
      ...resolveTitleVisual(key),
      measure: value === undefined ? null : this.measure(key, value),
    };
  }

  private progress(progress: RankingChallengeProgress): BoardProgress {
    const visual = resolveChallengeVisual(progress.metric, progress.difficulty);
    const current = this.value(progress.currentValue);
    const target = progress.targetValue === null ? null : this.value(progress.targetValue);
    const percent =
      progress.targetValue !== null && progress.targetValue > 0
        ? Math.min(100, (progress.currentValue / progress.targetValue) * 100)
        : progress.completed
          ? 100
          : 0;
    const tipKey = progress.completed ? 'cellTipDone' : target ? 'cellTip' : 'cellTipOpen';
    const category = resolveChallengeMetricLabel(progress.metric, (key) =>
      this.translation.translate(key),
    );
    const description = this.descriptions().get(progress.id);
    // The name, then what had to be done: the figures alone do not say what they count.
    const title = description ? `${progress.name} — ${description}` : progress.name;
    return {
      id: progress.id,
      mark: visual.tier,
      label: progress.name,
      name: `${title} · ${category}`,
      categoryLabel: category,
      currentValueLabel: current,
      compactValueLabel: this.value(progress.currentValue, true),
      targetValueLabel: target,
      completionPercentage: percent,
      completed: progress.completed,
      visual: { iconClass: visual.iconClass, badgeClass: visual.badgeClass },
      barClass: visual.barClass,
      tip: this.translation.translate(`leaderboard.board.${tipKey}`, {
        name: title,
        current,
        target: target ?? '',
        points: progress.rankingPoints,
      }),
    };
  }

  private value(amount: number, compact = false): string {
    return formatFigure(amount, this.locale(), compact);
  }

  private locale(): string {
    return resolveLocale(this.translation.language());
  }

  private weekSpan(weekStart: string): string {
    return formatWeekSpan(weekStart, this.locale());
  }
}
