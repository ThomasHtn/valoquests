import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { LucideChevronDown, LucideUsers } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import {
  CHALLENGE_DIFFICULTIES,
  ChallengeCatalogue,
  ChallengeProgress,
  CurrentChallenges,
} from '@core/challenges/challenge.model';
import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { localMidnight } from '@core/date/date-time.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionRule } from '@shared/section-rule/section-rule';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { ChallengeCardView } from './challenge-card/challenge-card';
import { ChallengeCatalogueView } from './challenge-catalogue/challenge-catalogue';
import { CatalogueGroup, ChallengeCard, DayCell, DayState, SquadSlot } from './challenges.model';
import { DailyFrieze } from './daily-frieze/daily-frieze';

const WEEK_DAYS = 7;

const DAILY_TONE = 'var(--color-accent-cyan)';

const CLOSED_DAY_TONE = 'var(--color-accent-green)';

/**
 * The ISO date `offset` days after another.
 */
function shiftDay(isoDate: string, offset: number): string {
  const date = localMidnight(isoDate);
  date.setDate(date.getDate() + offset);
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${`${date.getDate()}`.padStart(2, '0')}`;
}

/**
 * The week's challenges: what they are for, the day's one over the seven days, the week's five,
 * and the catalogue they were drawn from.
 *
 * Progress here is the squad's, one hexagon per operator: who has validated what. A reader's own
 * exact progress is on the leaderboard.
 */
@Component({
  selector: 'app-challenges',
  imports: [
    LucideUsers,
    TranslatePipe,
    PageHeader,
    ResourceState,
    SectionRule,
    DailyFrieze,
    ChallengeCardView,
    ChallengeCatalogueView,
    LucideChevronDown,
  ],
  templateUrl: './challenges.html',
  styleUrl: './challenges.css',
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
   * Whether validated challenges bring wounded home right now: only a running campaign has a base.
   */
  protected readonly rescueActive = computed(() => this.campaign()?.status === 'RUNNING');

  /**
   * What the week's challenges already secured: the wounded the campaign counts as acquired, and
   * the validations behind them. `null` outside a running campaign.
   */
  protected readonly acquired = computed<{ wounded: number; done: number; total: number } | null>(
    () => {
      const current = this.current();
      const forecast = this.campaign()?.forecast;
      if (!current || !this.rescueActive() || !forecast) {
        return null;
      }
      const done = current.challenges.reduce(
        (sum, challenge) => sum + challenge.completedPlayers,
        0,
      );
      const total = current.challenges.reduce((sum, challenge) => sum + challenge.totalPlayers, 0);
      return { wounded: forecast.challengeRescued, done, total };
    },
  );

  protected readonly hasChallenges = computed(() => {
    const current = this.current();
    return (current?.challenges.length ?? 0) + (current?.dailies.length ?? 0) > 0;
  });

  /**
   * Whether the reader unfolded the catalogue: nothing of it is fetched or rendered before.
   */
  protected readonly catalogueOpen = signal(false);

  /**
   * The week, then the campaign's tier when one is in force: the tier is what the targets and
   * rewards below were priced at.
   */
  protected readonly headerEyebrow = computed(() => {
    const campaign = this.campaign();
    const current = this.current();
    const week =
      campaign?.status === 'RUNNING' && campaign.currentWeekIndex !== null
        ? this.translation.translate('challenges.header.week', {
            week: campaign.currentWeekIndex,
            weeks: CAMPAIGN_WEEK_COUNT,
          })
        : current
          ? this.translation.translate('challenges.header.weekOf', {
              date: this.dayMonth(current.weekStart),
            })
          : this.translation.translate('challenges.title');
    const tier = campaign?.tier
      ? this.translation.translate('challenges.header.tier', {
          tier: this.translation.translate(`common.tier.${campaign.tier}`),
        })
      : '';
    return tier ? `${week} · ${tier}` : week;
  });

  /**
   * The empty state: the two draws that have not run, and when they do.
   */
  protected readonly emptyPlate = computed<EmptyPlate>(() => {
    const t = (suffix: string) => this.translation.translate(`challenges.state.empty.${suffix}`);
    return {
      illustration: 'draw',
      eyebrow: t('eyebrow'),
      title: t('title'),
      text: t('text'),
      readouts: [
        { tone: 'todo', label: t('weekly'), value: t('weeklyValue') },
        { tone: 'todo', label: t('daily'), value: t('dailyValue') },
      ],
    };
  });

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
      const slots = daily ? this.slots(current, daily) : [];
      const doneCount = slots.filter((slot) => slot.done).length;
      const weekdayFull = this.weekday(isoDate, 'long');
      const date = this.dayMonth(isoDate);
      return {
        index,
        state,
        weekday: this.weekday(isoDate, 'short'),
        date,
        card: daily ? this.dailyCard(daily, slots, state, weekdayFull, date) : null,
        doneCount,
        total: current.roster.length,
        tip: this.tip(state, daily !== null, doneCount, current.roster.length, weekdayFull),
      };
    });
  });

  protected readonly weeklyCards = computed<readonly ChallengeCard[]>(() => {
    const current = this.current();
    if (!current) {
      return [];
    }
    return current.challenges.map((challenge) => {
      const visual = resolveDifficultyVisual(challenge.difficulty);
      const slots = this.slots(current, challenge);
      return {
        tone: visual.tierColor,
        mark: visual.tier,
        kind: this.translation.translate(`common.difficulty.${challenge.difficulty ?? 'EASY'}`),
        // The one tier closed to an operator who never plays ranked: the rules want it said.
        aside: challenge.competitiveOnly
          ? this.translation.translate('challenges.card.competitiveOnly')
          : '',
        name: challenge.name,
        description: challenge.description,
        survivors: challenge.survivors,
        rankingPoints: challenge.rankingPoints,
        rescueActive: this.rescueActive(),
        slots,
        doneCount: slots.filter((slot) => slot.done).length,
      };
    });
  });

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
        label: this.translation.translate(`common.difficulty.${difficulty}`),
        entries: catalogue.challenges.filter(
          (entry) => entry.cadence === 'WEEKLY' && entry.difficulty === difficulty,
        ),
      };
    });
    return [daily, ...tiers].filter((group) => group.entries.length > 0);
  });

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

  private dayIndex(current: CurrentChallenges): number {
    const offset = Math.round(
      (localMidnight(current.today).getTime() - localMidnight(current.weekStart).getTime()) /
        86_400_000,
    );
    return Math.min(WEEK_DAYS - 1, Math.max(0, offset));
  }

  private slots(current: CurrentChallenges, challenge: ChallengeProgress): SquadSlot[] {
    return current.roster.map((operator) => ({
      name: operator.displayName,
      done: challenge.completedPlayerIds.includes(operator.id),
    }));
  }

  private dailyCard(
    daily: ChallengeProgress,
    slots: readonly SquadSlot[],
    state: DayState,
    weekday: string,
    date: string,
  ): ChallengeCard {
    return {
      tone: state === 'closed' ? CLOSED_DAY_TONE : DAILY_TONE,
      mark: 'D',
      kind: this.translation.translate('challenges.daily.key'),
      aside:
        state === 'closed'
          ? this.translation.translate('challenges.daily.closed', { weekday, date })
          : '',
      name: daily.name,
      description: daily.description,
      survivors: daily.survivors,
      rankingPoints: daily.rankingPoints,
      rescueActive: this.rescueActive(),
      slots,
      doneCount: slots.filter((slot) => slot.done).length,
    };
  }

  private tip(
    state: DayState,
    drawn: boolean,
    count: number,
    total: number,
    weekday: string,
  ): string {
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`challenges.daily.${key}`, params);
    if (state === 'ahead') {
      return t('tipAhead', { weekday });
    }
    if (!drawn) {
      return state === 'now' ? t('tipNotYet') : t('tipMissing', { weekday });
    }
    const today = state === 'now' ? 'Today' : '';
    return count === 0
      ? t(`tipNone${today}`, { total, weekday })
      : t(`tipDone${today}`, { count, total, weekday });
  }

  private locale(): string {
    return this.translation.language() === 'fr' ? 'fr-FR' : 'en-US';
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
    return new Intl.DateTimeFormat(this.locale(), { day: 'numeric', month: 'short' }).format(
      localMidnight(isoDate),
    );
  }
}
