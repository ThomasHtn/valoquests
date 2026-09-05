import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCalendar, LucideRocket, LucideSkull, LucideUsers } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import {
  Campaign,
  CAMPAIGN_WEEK_COUNT,
  CampaignWeek,
  WeeklyTitle,
} from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { PageHeader } from '@layout/page-header/page-header';
import { CountUp } from '@shared/count-up/count-up';
import { Countdown } from '@shared/countdown/countdown';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionRule } from '@shared/section-rule/section-rule';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { BaseScene } from './base-scene/base-scene';
import { DayOrders } from './day-orders/day-orders';
import { ExtractionGauges } from './extraction-gauges/extraction-gauges';
import { Capacity, DailyOrder, DayTally, FriezeWeek, Mission, SquadRow } from './overview.model';
import { PlanetFigure } from './planet-figure/planet-figure';
import { ScanWires } from './scan-wires';
import { SquadSheet } from './squad-sheet/squad-sheet';

/**
 * Population a campaign run to its end is expected to reach at the normal tier: the scale the
 * city grows on, so a base that went the distance fills its whole skyline.
 */
const FULL_CAMPAIGN_POPULATION = 30_000;

/**
 * Milliseconds in a day.
 */
const DAY_MS = 86_400_000;

/**
 * Parses an ISO date (`YYYY-MM-DD`) as local midnight.
 */
function localMidnight(isoDate: string, plusDays = 0): number {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day + plusDays).getTime();
}

/**
 * Whole days from one ISO date to another.
 */
function daysBetween(from: string, to: string): number {
  return Math.round((localMidnight(to) - localMidnight(from)) / DAY_MS);
}

/**
 * The state of the campaign, at a glance: the base and its rocket, the ten weeks, the mission of
 * the week, the orders of the day and what each operator brought in today.
 *
 * A screen of states, never of advice: each figure is doubled by what it pays for in people, and
 * the rule of Sunday's settlement belongs to the campaign page.
 */
@Component({
  selector: 'app-overview',
  imports: [
    LowerCasePipe,
    TranslatePipe,
    RouterLink,
    PageHeader,
    ResourceState,
    SectionRule,
    Countdown,
    CountUp,
    BaseScene,
    PlanetFigure,
    ScanWires,
    ExtractionGauges,
    DayOrders,
    SquadSheet,
    LucideCalendar,
    LucideRocket,
    LucideSkull,
    LucideUsers,
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Overview {
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  protected readonly fullCampaignPopulation = FULL_CAMPAIGN_POPULATION;

  private readonly campaignApi = inject(CampaignApi);

  private readonly challengesApi = inject(ChallengesApi);

  private readonly rankingApi = inject(RankingApi);

  private readonly translation = inject(Translation);

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly todayResource = this.campaignApi.today;

  protected readonly challengesResource = this.challengesApi.current;

  protected readonly rankingResource = this.rankingApi.current;

  protected readonly dailyResource = this.rankingApi.daily;

  protected readonly isLoading = anyLoading(
    this.campaignResource,
    this.todayResource,
    this.challengesResource,
    this.rankingResource,
    this.dailyResource,
  );

  protected readonly isError = anyError(
    this.campaignResource,
    this.todayResource,
    this.challengesResource,
    this.rankingResource,
    this.dailyResource,
  );

  protected readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * The week in progress, or `null` outside a running campaign's ten weeks.
   */
  protected readonly currentWeek = computed<CampaignWeek | null>(() => {
    const campaign = this.campaign();
    if (campaign?.status !== 'RUNNING' || campaign.currentWeekIndex === null) {
      return null;
    }
    return campaign.weeks[campaign.currentWeekIndex - 1] ?? null;
  });

  /**
   * Whether the page can show a mission: a running campaign, inside its weeks, with a forecast.
   */
  protected readonly isRunning = computed(
    () => this.currentWeek() !== null && this.campaign()?.forecast !== null,
  );

  protected readonly headerEyebrow = computed(() => {
    const campaign = this.campaign();
    if (campaign?.number === null || campaign?.tier === null || !campaign) {
      return this.translation.translate('overview.header.noCampaign');
    }
    return this.translation.translate('overview.header.eyebrow', {
      number: campaign.number,
      tier: this.translation.translate(`common.tier.${campaign.tier}`),
    });
  });

  protected readonly dayLabel = computed(() => {
    const campaign = this.campaign();
    if (!campaign) {
      return '';
    }
    const weekday = new Intl.DateTimeFormat(this.locale(), { weekday: 'long' }).format(
      new Date(localMidnight(campaign.today)),
    );
    const capitalised = weekday.charAt(0).toUpperCase() + weekday.slice(1);
    if (campaign.currentWeekIndex === null) {
      return capitalised;
    }
    return this.translation.translate('overview.header.day', {
      weekday: capitalised,
      week: campaign.currentWeekIndex,
      weeks: CAMPAIGN_WEEK_COUNT,
    });
  });

  protected readonly population = computed(() => this.campaign()?.base?.population ?? 0);

  protected readonly populationChange = computed(
    () => this.campaign()?.base?.populationChange ?? 0,
  );

  protected readonly stagesDone = computed(() => this.campaign()?.totals?.guardiansDefeated ?? 0);

  protected readonly sceneLabel = computed(() =>
    this.translation.translate('overview.scene.aria', {
      population: this.format(this.population()),
      stages: this.stagesDone(),
      weeks: CAMPAIGN_WEEK_COUNT,
    }),
  );

  protected readonly frieze = computed<readonly FriezeWeek[]>(() => {
    const campaign = this.campaign();
    if (!campaign || campaign.weeks.length === 0) {
      return [];
    }
    return campaign.weeks.map((week) => this.toFriezeWeek(week, campaign));
  });

  protected readonly mission = computed<Mission | null>(() => {
    const campaign = this.campaign();
    const week = this.currentWeek();
    if (!campaign || !week) {
      return null;
    }
    const hitPointsLeft = Math.max(0, week.guardianHitPoints - week.damageDealt);
    return {
      weekIndex: week.weekIndex,
      planetName: week.planetName,
      category: week.category,
      dayOfWeek: Math.min(7, Math.max(1, daysBetween(week.weekStart, campaign.today) + 1)),
      guardianName: week.guardianName ?? '',
      hitPointsLeft,
      hitPoints: week.guardianHitPoints,
      breachPercent: week.progressPercent,
      guardianLeft: week.guardianHitPoints > 0 ? hitPointsLeft / week.guardianHitPoints : 0,
      wounded: week.woundedCount,
      crew: campaign.rosterSize ?? 0,
      extractionDeadline: localMidnight(week.weekStart, 7),
    };
  });

  protected readonly capacity = computed<Capacity | null>(() => {
    const campaign = this.campaign();
    const week = this.currentWeek();
    const base = campaign?.base;
    const forecast = campaign?.forecast;
    if (!campaign || !week || !base || !forecast) {
      return null;
    }
    const wounded = Math.max(1, week.woundedCount);
    const fraction = (value: number): number => Math.min(1, value / wounded);
    return {
      wounded: week.woundedCount,
      carry: {
        value: base.rescuesByComponents,
        fraction: fraction(base.rescuesByComponents),
        stock: base.componentsStock,
      },
      shelter: {
        value: base.rescuesByFood,
        fraction: fraction(base.rescuesByFood),
        stock: base.foodStock,
      },
      breach: {
        value: week.progressPercent,
        fraction: week.progressPercent / 100,
        stock: Math.max(0, week.guardianHitPoints - week.damageDealt),
      },
      aboard: forecast.rescued,
      aboardFraction: fraction(forecast.rescued),
      fromGuardian: forecast.extractionRescued,
      fromChallenges: forecast.challengeRescued,
      leftBehind: forecast.leftBehind,
      limiter: forecast.limiter,
      componentsPerRescue: base.componentsPerRescue,
      foodPerRescue: base.foodPerRescue,
      hitPointsPerPercent: Math.round(week.guardianHitPoints / 100),
    };
  });

  protected readonly dailyOrder = computed<DailyOrder | null>(() => {
    const challenges = resourceValue(this.challengesResource, null);
    const ranking = resourceValue(this.rankingResource, null);
    if (!challenges) {
      return null;
    }
    const daily = challenges.dailies.find((entry) => entry.day === challenges.today) ?? null;
    if (!daily) {
      return null;
    }
    const validated = (ranking?.ranking ?? [])
      .filter((entry) => entry.position !== null)
      .map((entry) => ({
        name: entry.player.displayName,
        done:
          entry.challengeProgress.find((line) => line.cadence === 'DAILY' && line.id === daily.id)
            ?.completed ?? false,
      }));
    return {
      name: daily.name,
      description: daily.description,
      survivors: daily.survivors,
      validated,
      doneCount: validated.filter((operator) => operator.done).length,
      deadline: localMidnight(challenges.today, 1),
    };
  });

  protected readonly tally = computed<DayTally | null>(() => {
    const today = resourceValue(this.todayResource, null);
    const week = this.currentWeek();
    const base = this.campaign()?.base;
    if (!today || !week || !base) {
      return null;
    }
    return {
      guardianName: week.guardianName ?? '',
      damage: today.damage,
      components: today.components,
      carryGained: today.carryGained,
      food: today.food,
      shelterGained: today.shelterGained,
      upkeep: today.dailyUpkeep,
      population: base.population,
      presence: today.presenceCount,
      roster: today.rosterSize,
      pips: Array.from({ length: today.rosterSize }, (_, index) => index < today.presenceCount),
    };
  });

  protected readonly squad = computed<readonly SquadRow[]>(() => {
    const daily = resourceValue(this.dailyResource, null);
    const today = resourceValue(this.todayResource, null);
    if (!daily) {
      return [];
    }
    const titlesByPlayer = new Map<number, WeeklyTitle>();
    for (const [title, playerId] of Object.entries(today?.titles ?? {})) {
      if (playerId !== undefined && !titlesByPlayer.has(playerId)) {
        titlesByPlayer.set(playerId, title as WeeklyTitle);
      }
    }
    return daily.ranking.map((entry) => {
      const title = titlesByPlayer.get(entry.playerId) ?? null;
      const played = entry.matchCount > 0;
      return {
        position: played ? entry.position : null,
        playerId: entry.playerId,
        name: entry.displayName,
        portrait: resolvePlayerAvatarUrl(entry.portrait),
        title: title === null ? null : { key: title, ...resolveTitleVisual(title) },
        played,
        streakMultiplier: this.formatMultiplier(
          played ? entry.streakBonusPercent : this.streakBonusOf(entry.streakAtStake),
        ),
        streakDays: played ? entry.streakDays : entry.streakAtStake,
        streakAtStake: entry.streakAtStake,
        damage: entry.damage,
        matchCount: entry.matchCount,
        reducedMatchCount: entry.reducedMatchCount,
        components: entry.components,
        food: entry.food,
      };
    });
  });

  protected readonly rosterCount = computed(
    () => resourceValue(this.dailyResource, null)?.rosterPlayerCount ?? 0,
  );

  protected readonly playerCount = computed(
    () => resourceValue(this.dailyResource, null)?.ranking.length ?? 0,
  );

  /**
   * What the empty state says when there is no mission to show.
   */
  protected readonly stateKey = computed(() => {
    const campaign = this.campaign();
    if (!campaign || campaign.status === null) {
      return 'none';
    }
    if (campaign.status === 'OPENED' || campaign.currentWeekIndex === null) {
      return 'opened';
    }
    return campaign.status === 'CLOSED' ? 'closed' : 'settling';
  });

  protected retry(): void {
    reloadAll(
      this.campaignResource,
      this.todayResource,
      this.challengesResource,
      this.rankingResource,
      this.dailyResource,
    );
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected signed(amount: number): string {
    const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
    return `${sign}${this.format(Math.abs(amount))}`;
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }

  private locale(): string {
    return this.translation.language() === 'fr' ? 'fr-FR' : 'en-US';
  }

  private formatMultiplier(bonusPercent: number): string {
    return `×${new Intl.NumberFormat(this.locale(), {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(1 + bonusPercent / 100)}`;
  }

  /**
   * Bonus a streak of that many days pays: nothing on the first day, two percent per day after,
   * capped at ten — the barème's ladder, restated for an operator who has not played yet and
   * whose streak the daily board therefore does not price.
   */
  private streakBonusOf(streakDays: number): number {
    return Math.max(0, Math.min(10, (streakDays - 1) * 2));
  }

  private toFriezeWeek(week: CampaignWeek, campaign: Campaign): FriezeWeek {
    const isCurrent = week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING';
    const label = String(week.weekIndex).padStart(2, '0');
    if (week.defeated) {
      return {
        index: week.weekIndex,
        label,
        state: 'won',
        advance: 1,
        mark: '✓',
        title: this.translation.translate('overview.frieze.won'),
      };
    }
    if (week.settled) {
      return {
        index: week.weekIndex,
        label,
        state: 'lost',
        advance: week.progressPercent / 100,
        mark: '✕',
        title: this.translation.translate('overview.frieze.lost', {
          percent: week.progressPercent,
        }),
      };
    }
    if (isCurrent) {
      return {
        index: week.weekIndex,
        label,
        state: 'now',
        advance: week.progressPercent / 100,
        mark: '●',
        title: this.translation.translate('overview.frieze.now', {
          percent: week.progressPercent,
        }),
      };
    }
    // A closed campaign's remaining weeks were never played: they are not coming any more.
    const unplayed = campaign.status === 'CLOSED';
    return {
      index: week.weekIndex,
      label,
      state: 'ahead',
      advance: 0,
      mark: week.weekIndex === CAMPAIGN_WEEK_COUNT && !unplayed ? '★' : '·',
      title: this.translation.translate(
        unplayed ? 'overview.frieze.unplayed' : 'overview.frieze.ahead',
      ),
    };
  }
}
