import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';

import {
  campaignMidnight,
  remainingWeekTime,
  weekDayIndex,
} from '@core/campaign/calendar/campaign-calendar.utils';
import { CampaignApi } from '@core/campaign/campaign-api';
import { DAILY_TONE } from '@core/challenges/card/challenge-card.constants';
import {
  BoardRow,
  ChallengeLook,
  ChallengeOperator,
} from '@core/challenges/card/challenge-card.model';
import {
  buildChallengeCard,
  toBoardRow,
  toOperators,
} from '@core/challenges/card/challenge-card.utils';
import { CHALLENGE_DIFFICULTIES } from '@core/challenges/challenge.constants';
import { ChallengeProgress, CurrentChallenges } from '@core/challenges/challenge.model';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { resolveDifficultyVisual } from '@core/challenges/visual/challenge-visual.utils';
import { WEEK_DAYS } from '@core/date/date.constants';
import { RemainingTime } from '@core/date/date.model';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolveLocale } from '@core/i18n/format/locale.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TranslateFn } from '@core/i18n/translation.model';
import { readPinnedPlayer, writePinnedPlayer } from '@core/players/pin/player-pin.utils';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';

import { ChallengeBoard } from './challenge-board/challenge-board';
import { ChallengeCatalogueView } from './challenge-catalogue/challenge-catalogue';
import { ChallengeDeck } from './challenge-deck/challenge-deck';
import {
  BoardOperator,
  CatalogueGroup,
  DayCell,
  DayPickSource,
  DayState,
} from './challenges.model';
import {
  formatDayMonth,
  formatDayOfMonth,
  formatWeekday,
  orderOperators,
  resolvePickedDay,
  shiftDay,
} from './challenges.utils';

/**
 * Challenges page: table when wide, cards below, both fed the same rows and picks.
 */
@Component({
  selector: 'app-challenges',
  imports: [
    TranslatePipe,
    PageHeader,
    ResourceState,
    WeekCountdown,
    ChallengeBoard,
    ChallengeDeck,
    ChallengeCatalogueView,
  ],
  templateUrl: './challenges.html',
  styleUrl: './challenges.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Challenges {
  /**
   * Challenges feed, for the week's draws and the catalogue.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Campaign feed, to know whether validations bring wounded home.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Translation service, for the labels built in code.
   */
  private readonly translation = inject(Translation);

  /**
   * The week's challenges and dailies with everyone's progress.
   */
  private readonly challengesResource = this.challengesApi.current;

  /**
   * The campaign, read for its status.
   */
  private readonly campaignResource = this.campaignApi.campaign;

  /**
   * Every challenge the draws can hand out, fetched once the fold opens.
   */
  private readonly catalogueResource = this.challengesApi.catalogue;

  /**
   * Whether the board still waits on the challenges or the campaign.
   */
  protected readonly isLoading = anyLoading(this.challengesResource, this.campaignResource);

  /**
   * Whether the challenges or the campaign failed to load.
   */
  protected readonly isError = anyError(this.challengesResource, this.campaignResource);

  /**
   * Whether the catalogue is still loading, kept apart so the board shows meanwhile.
   */
  protected readonly catalogueLoading = anyLoading(this.catalogueResource);

  /**
   * Whether the catalogue failed, retried on its own.
   */
  protected readonly catalogueError = anyError(this.catalogueResource);

  /**
   * The week's challenges, or `null` until loaded.
   */
  private readonly current = computed<CurrentChallenges | null>(
    () => resourceValue(this.challengesResource, null) ?? null,
  );

  /**
   * The campaign, or `null` until loaded.
   */
  private readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * The clock the week's countdown reads, refreshed every minute.
   */
  private readonly now = signal(Date.now());

  /**
   * Whether challenges bring wounded home: only a running campaign has a base.
   */
  protected readonly rescueActive = computed(() => this.campaign()?.status === 'RUNNING');

  /**
   * Whether anything is drawn yet, else the empty state shows.
   */
  protected readonly hasChallenges = computed(() => {
    const current = this.current();
    return (current?.challenges.length ?? 0) + (current?.dailies.length ?? 0) > 0;
  });

  /**
   * Time left before the week closes, or `null` while the week is loading.
   */
  protected readonly remaining = computed<RemainingTime | null>(() => {
    const current = this.current();
    return current ? remainingWeekTime(current.weekEnd, new Date(this.now())) : null;
  });

  /**
   * The week's first and last days for the heading, empty while loading.
   */
  protected readonly period = computed(() => {
    const current = this.current();
    if (!current) {
      return '';
    }
    return this.translation.translate('challenges.board.period', {
      start: formatDayMonth(current.weekStart, this.locale()),
      end: formatDayMonth(shiftDay(current.weekStart, WEEK_DAYS - 1), this.locale()),
    });
  });

  /**
   * Midnight tonight in the campaign time zone, when the day's challenge closes.
   */
  private readonly dailyDeadline = computed(() => {
    const current = this.current();
    return current ? campaignMidnight(current.today, 1).getTime() : 0;
  });

  /**
   * Whether the catalogue is unfolded; nothing of it is fetched or rendered before.
   */
  protected readonly catalogueOpen = signal(false);

  /**
   * Empty state before the draws.
   */
  protected readonly emptyPlate = computed<EmptyPlate>(() => {
    const t = (suffix: string) => this.translation.translate(`challenges.state.empty.${suffix}`);
    return {
      illustration: 'draw',
      title: t('title'),
      text: t('text'),
      readouts: [],
    };
  });

  /**
   * The roster the cards line up.
   */
  private readonly operators = computed<readonly ChallengeOperator[]>(() =>
    toOperators(this.current()?.roster ?? []),
  );

  /**
   * The week's seven days for the daily row, with their state and tally.
   */
  protected readonly days = computed<readonly DayCell[]>(() => {
    const current = this.current();
    if (!current) {
      return [];
    }
    const todayIndex = weekDayIndex(current.weekStart, current.today);
    return Array.from({ length: WEEK_DAYS }, (_, index) => {
      const isoDate = shiftDay(current.weekStart, index);
      const state: DayState =
        index < todayIndex ? 'closed' : index === todayIndex ? 'now' : 'ahead';
      const daily = current.dailies.find((entry) => entry.day === isoDate) ?? null;
      const doneCount = daily
        ? current.roster.filter((operator) => daily.completedPlayerIds.includes(operator.id)).length
        : 0;
      const tip = this.tip(daily !== null, state, doneCount, current.roster.length);
      return {
        index,
        state,
        weekday: formatWeekday(isoDate, this.locale(), 'short'),
        date: formatDayOfMonth(isoDate, this.locale()),
        drawn: daily !== null,
        doneCount,
        total: current.roster.length,
        tip: daily
          ? this.translation.translate('challenges.daily.tipNamed', { name: daily.name, tip })
          : tip,
      };
    });
  });

  /**
   * Day the daily row shows: today until the reader picks another; a reload keeps the pick.
   */
  protected readonly pickedDay = linkedSignal<DayPickSource, number | null>({
    source: () => ({ weekStart: this.current()?.weekStart ?? null, days: this.days() }),
    computation: (source, previous) => resolvePickedDay(source, previous),
  });

  /**
   * Whether the card shows today's still-running challenge.
   */
  private readonly showingToday = computed(
    () => this.days().find((day) => day.index === this.pickedDay())?.state === 'now',
  );

  /**
   * The picked day's challenge, or `null` while none is drawn.
   */
  private readonly dailyRow = computed<BoardRow | null>(() => {
    const current = this.current();
    const index = this.pickedDay();
    if (!current || index === null) {
      return null;
    }
    const isoDate = shiftDay(current.weekStart, index);
    const daily = current.dailies.find((entry) => entry.day === isoDate) ?? null;
    if (!daily) {
      return null;
    }
    const kind =
      isoDate === current.today
        ? this.translation.translate('challenges.daily.key')
        : this.translation.translate('challenges.daily.keyOn', {
            weekday: formatWeekday(isoDate, this.locale(), 'long'),
            date: formatDayOfMonth(isoDate, this.locale()),
          });
    const closesAt = this.showingToday() ? this.dailyDeadline() : null;
    return this.row(daily, { tone: DAILY_TONE, mark: 'D', kind }, closesAt);
  });

  /**
   * The week's challenges as board rows, shown under the daily one.
   */
  private readonly weeklyRows = computed<readonly BoardRow[]>(() =>
    (this.current()?.challenges ?? []).map((challenge) => {
      const visual = resolveDifficultyVisual(challenge.tier);
      const kind = this.translation.translate(
        `common.challengeDifficulty.${challenge.tier ?? 'EASY'}`,
      );
      return this.row(challenge, { tone: visual.tierColor, mark: visual.tier, kind }, null);
    }),
  );

  /**
   * Operator pinned first, remembered across visits.
   */
  private readonly pinned = signal<number | null>(readPinnedPlayer());

  /**
   * Board order: the pinned operator, then the furthest along.
   */
  private readonly boardOrder = computed<readonly ChallengeOperator[]>(() =>
    orderOperators(this.operators(), this.current()?.challenges ?? [], this.pinned()),
  );

  /**
   * Board columns: operators in board order with their reward and tooltip summary.
   */
  protected readonly boardOperators = computed<readonly BoardOperator[]>(() => {
    const current = this.current();
    if (!current) {
      return [];
    }
    const rescue = this.rescueActive();
    const drawn = [...current.challenges, ...current.dailies];
    return this.boardOrder().map((operator) => {
      const validated = (challenge: ChallengeProgress): boolean =>
        challenge.players.some((line) => line.playerId === operator.playerId && line.completed);
      const weeklyDone = current.challenges.filter(validated).length;
      const reward = drawn
        .filter(validated)
        .reduce((sum, challenge) => sum + challenge.survivors, 0);
      const t: TranslateFn = (key, params) =>
        this.translation.translate(`challenges.board.summary.${key}`, params);
      const summary = [
        t('done', { name: operator.name, count: weeklyDone, total: current.challenges.length }),
        t(rescue ? 'wounded' : 'points', { count: reward }),
      ].join(', ');
      return {
        ...operator,
        reward,
        pinned: operator.playerId === this.pinned(),
        summary,
      };
    });
  });

  /**
   * The picked day's challenge, then the week's five.
   */
  protected readonly boardRows = computed<readonly BoardRow[]>(() => {
    const daily = this.dailyRow();
    return daily ? [daily, ...this.weeklyRows()] : this.weeklyRows();
  });

  /**
   * The catalogue split into the daily pool and the five difficulties, empty ones dropped.
   */
  protected readonly catalogueGroups = computed<readonly CatalogueGroup[]>(() => {
    const catalogue = resourceValue(this.catalogueResource, null) ?? null;
    if (!catalogue) {
      return [];
    }
    const daily: CatalogueGroup = {
      key: 'DAILY',
      tone: DAILY_TONE,
      mark: 'D',
      label: this.translation.translate('challenges.catalogue.daily'),
      entries: catalogue.challenges.filter((entry) => entry.cadence === 'DAILY'),
    };
    const tiers = CHALLENGE_DIFFICULTIES.map((tier): CatalogueGroup => {
      const visual = resolveDifficultyVisual(tier);
      return {
        key: tier,
        tone: visual.tierColor,
        mark: visual.tier,
        label: this.translation.translate(`common.challengeDifficulty.${tier}`),
        entries: catalogue.challenges.filter(
          (entry) => entry.cadence === 'WEEKLY' && entry.tier === tier,
        ),
      };
    });
    return [daily, ...tiers].filter((group) => group.entries.length > 0);
  });

  /**
   * Ticks the countdown's clock every minute until the page closes.
   */
  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  /**
   * Reloads the board after a failure.
   */
  protected retry(): void {
    reloadAll(this.challengesResource, this.campaignResource);
  }

  /**
   * Reloads the catalogue after a failure.
   */
  protected retryCatalogue(): void {
    reloadAll(this.catalogueResource);
  }

  /**
   * Requests the catalogue the first time the fold opens.
   */
  protected toggleCatalogue(event: Event): void {
    const open = (event.target as HTMLDetailsElement).open;
    this.catalogueOpen.set(open);
    if (open) {
      this.challengesApi.catalogueRequested.set(true);
    }
  }

  /**
   * Pins an operator first on the board, or unpins them when already pinned.
   */
  protected togglePin(playerId: number): void {
    const next = this.pinned() === playerId ? null : playerId;
    this.pinned.set(next);
    writePinnedPlayer(next);
  }

  /**
   * Board row with marks in board order; `closesAt` is `null` unless the day still runs.
   */
  private row(
    challenge: ChallengeProgress,
    look: ChallengeLook,
    closesAt: number | null,
  ): BoardRow {
    const language = this.translation.language();
    const card = buildChallengeCard(
      challenge,
      look,
      this.operators(),
      this.rescueActive(),
      (amount) => formatFigure(amount, language, amount >= 1_000),
    );
    const rungs = new Map(card.rungs.map((rung) => [rung.playerId, rung]));
    const ordered = this.boardOrder().flatMap((operator) => rungs.get(operator.playerId) ?? []);
    return toBoardRow(challenge, card, ordered, closesAt, language, (key, params) =>
      this.translation.translate(key, params),
    );
  }

  /**
   * Hover text of a day cell: who validated its challenge, or why there is none.
   */
  private tip(drawn: boolean, state: DayState, count: number, total: number): string {
    const t: TranslateFn = (key, params) =>
      this.translation.translate(`challenges.daily.${key}`, params);
    // Undrawn: a day ahead says when it opens, any other day is unavailable.
    if (!drawn) {
      return t(state === 'ahead' ? 'tipAhead' : 'tipUnavailable');
    }
    return count === 0 ? t('tipNone', { total }) : t('tipDone', { count, total });
  }

  /**
   * Locale of the chosen language, for date formats.
   */
  private locale(): string {
    return resolveLocale(this.translation.language());
  }
}
