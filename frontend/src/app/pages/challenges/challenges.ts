import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { CampaignApi } from '@core/campaign/campaign-api';
import {
  CHALLENGE_DIFFICULTIES,
  ChallengeCatalogue,
  ChallengeProgress,
  CurrentChallenges,
} from '@core/challenges/challenge.model';
import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { campaignMidnight } from '@core/date/campaign-time-zone.utils';
import { WEEK_DAYS } from '@core/date/date-time.constants';
import { localMidnight } from '@core/date/date-time.utils';
import { RemainingTime, remainingWeekTime } from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolveLocale } from '@core/i18n/locale.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { Countdown } from '@shared/countdown/countdown';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionRule } from '@shared/section-rule/section-rule';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { formatFigure } from '../leaderboard/leaderboard-board.utils';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { ChallengeCardView } from './challenge-card/challenge-card';
import { ChallengeCatalogueView } from './challenge-catalogue/challenge-catalogue';
import { DAILY_TONE } from './challenges.constants';
import {
  CatalogueGroup,
  ChallengeCard,
  ChallengeLook,
  ChallengeOperator,
  DayCell,
  DayState,
} from './challenges.model';
import { buildChallengeCard, shiftDay, toOperators } from './challenges.utils';
import { DailyWeek } from './daily-week/daily-week';

/**
 * The week's challenges: the day's one beside the seven days of the week, the week's five, and the
 * catalogue they were drawn from.
 *
 * Every card lists the squad operator by operator, each on a band closing toward the target, so
 * the page reads who is close as much as who is done.
 */
@Component({
  selector: 'app-challenges',
  imports: [
    TranslatePipe,
    PageHeader,
    ResourceState,
    SectionRule,
    WeekCountdown,
    Countdown,
    DailyWeek,
    ChallengeCardView,
    ChallengeCatalogueView,
  ],
  templateUrl: './challenges.html',
  styleUrl: './challenges.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Challenges {
  private readonly challengesApi = inject(ChallengesApi);

  private readonly campaignApi = inject(CampaignApi);

  private readonly translation = inject(Translation);

  protected readonly challengesResource = this.challengesApi.current;

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly catalogueResource = this.challengesApi.catalogue;

  protected readonly isLoading = anyLoading(this.challengesResource, this.campaignResource);

  protected readonly isError = anyError(this.challengesResource);

  protected readonly catalogueLoading = anyLoading(this.catalogueResource);

  protected readonly catalogueError = anyError(this.catalogueResource);

  protected readonly current = computed<CurrentChallenges | null>(
    () => resourceValue(this.challengesResource, null) ?? null,
  );

  private readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * The clock the week's countdown reads, refreshed every minute.
   */
  private readonly now = signal(Date.now());

  /**
   * Whether validated challenges bring wounded home right now: only a running campaign has a base.
   */
  protected readonly rescueActive = computed(() => this.campaign()?.status === 'RUNNING');

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
   * Midnight tonight in the campaign time zone, when the day's challenge closes.
   */
  protected readonly dailyDeadline = computed(() => {
    const current = this.current();
    return current ? campaignMidnight(current.today, 1).getTime() : 0;
  });

  /**
   * Whether the reader unfolded the catalogue: nothing of it is fetched or rendered before.
   */
  protected readonly catalogueOpen = signal(false);

  /**
   * The empty state: the two draws that have not run, and when they do.
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

  protected readonly days = computed<readonly DayCell[]>(() => {
    const current = this.current();
    if (!current) {
      return [];
    }
    const todayIndex = this.dayIndex(current);
    return Array.from({ length: WEEK_DAYS }, (_, index) => {
      const isoDate = shiftDay(current.weekStart, index);
      const state: DayState =
        index < todayIndex ? 'closed' : index === todayIndex ? 'now' : 'ahead';
      const daily = current.dailies.find((entry) => entry.day === isoDate) ?? null;
      const doneCount = daily
        ? current.roster.filter((operator) => daily.completedPlayerIds.includes(operator.id)).length
        : 0;
      const tip = this.tip(daily !== null, doneCount, current.roster.length);
      return {
        index,
        state,
        weekday: this.weekday(isoDate, 'short'),
        date: this.dayMonth(isoDate),
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
   * Index of the day whose challenge the card shows. Follows today until the reader picks another
   * day on the strip, and again once the week rolls.
   */
  protected readonly pickedDay = linkedSignal<number | null>(
    () => this.days().find((day) => day.state === 'now')?.index ?? null,
  );

  /**
   * Whether the card shows today's challenge, which still runs, rather than a closed day's.
   */
  protected readonly showingToday = computed(
    () => this.days().find((day) => day.index === this.pickedDay())?.state === 'now',
  );

  /**
   * The picked day's challenge, or `null` while today's is not drawn yet.
   */
  protected readonly dailyCard = computed<ChallengeCard | null>(() => {
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
            weekday: this.weekday(isoDate, 'long'),
            date: this.dayMonth(isoDate),
          });
    return this.card(daily, { tone: DAILY_TONE, mark: 'D', kind });
  });

  protected readonly weeklyCards = computed<readonly ChallengeCard[]>(() =>
    (this.current()?.challenges ?? []).map((challenge) => {
      const visual = resolveDifficultyVisual(challenge.difficulty);
      return this.card(challenge, {
        tone: visual.tierColor,
        mark: visual.tier,
        kind: this.translation.translate(
          `common.challengeDifficulty.${challenge.difficulty ?? 'EASY'}`,
        ),
      });
    }),
  );

  protected readonly catalogueGroups = computed<readonly CatalogueGroup[]>(() => {
    const catalogue: ChallengeCatalogue | null =
      resourceValue(this.catalogueResource, null) ?? null;
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
    const tiers = CHALLENGE_DIFFICULTIES.map((difficulty): CatalogueGroup => {
      const visual = resolveDifficultyVisual(difficulty);
      return {
        key: difficulty,
        tone: visual.tierColor,
        mark: visual.tier,
        label: this.translation.translate(`common.challengeDifficulty.${difficulty}`),
        entries: catalogue.challenges.filter(
          (entry) => entry.cadence === 'WEEKLY' && entry.difficulty === difficulty,
        ),
      };
    });
    return [daily, ...tiers].filter((group) => group.entries.length > 0);
  });

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected retry(): void {
    reloadAll(this.challengesResource, this.campaignResource);
  }

  protected retryCatalogue(): void {
    reloadAll(this.catalogueResource);
  }

  /**
   * Follows the native fold: the catalogue is asked for the first time it opens.
   */
  protected toggleCatalogue(event: Event): void {
    const open = (event.target as HTMLDetailsElement).open;
    this.catalogueOpen.set(open);
    if (open) {
      this.challengesApi.catalogueRequested.set(true);
    }
  }

  private card(challenge: ChallengeProgress, look: ChallengeLook): ChallengeCard {
    return buildChallengeCard(challenge, look, this.operators(), this.rescueActive(), (amount) =>
      formatFigure(amount, this.locale(), amount >= 1_000),
    );
  }

  private dayIndex(current: CurrentChallenges): number {
    const offset = Math.round(
      (localMidnight(current.today).getTime() - localMidnight(current.weekStart).getTime()) /
        86_400_000,
    );
    return Math.min(WEEK_DAYS - 1, Math.max(0, offset));
  }

  private tip(drawn: boolean, count: number, total: number): string {
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`challenges.daily.${key}`, params);
    // A day ahead, a day the tick missed and today before its draw all read the same.
    if (!drawn) {
      return t('tipUnavailable');
    }
    return count === 0 ? t('tipNone', { total }) : t('tipDone', { count, total });
  }

  private locale(): string {
    return resolveLocale(this.translation.language());
  }

  private weekday(isoDate: string, width: 'short' | 'long'): string {
    const label = new Intl.DateTimeFormat(this.locale(), { weekday: width }).format(
      localMidnight(isoDate),
    );
    return width === 'short'
      ? label.replace('.', '').charAt(0).toUpperCase() + label.replace('.', '').slice(1)
      : label;
  }

  private dayMonth(isoDate: string): string {
    return new Intl.DateTimeFormat(this.locale(), { day: 'numeric' }).format(
      localMidnight(isoDate),
    );
  }
}
