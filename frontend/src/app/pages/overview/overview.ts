import { NgOptimizedImage, NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { LucideDynamicIcon, LucideFileText } from '@lucide/angular';

import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { CampaignApi } from '@core/campaign/campaign-api';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { CountUp } from '@shared/count-up/count-up';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionRule } from '@shared/section-rule/section-rule';

import { BaseScene } from './base-scene/base-scene';
import { readSeenPopulation } from './base-scene/base-scene.utils';
import { CampaignPanel } from './campaign-panel/campaign-panel';
import { DayOrders } from './day-orders/day-orders';
import { DayTally } from './day-orders/day-orders.model';
import { buildDailyRow, buildTally } from './day-orders/day-orders.utils';
import { ExtractionGauges } from './extraction-gauges/extraction-gauges';
import { Capacity } from './extraction-gauges/extraction-gauges.model';
import { buildCapacity } from './extraction-gauges/extraction-gauges.utils';
import { GuardianFall } from './mission-readings/fall-forecast/fall-forecast.model';
import { buildGuardianFall } from './mission-readings/fall-forecast/fall-forecast.utils';
import { MissionReadings } from './mission-readings/mission-readings';
import { Contribution, Mission, SundayStakes } from './mission-readings/mission-readings.model';
import {
  buildContribution,
  buildMission,
  buildSundayStakes,
} from './mission-readings/mission-readings.utils';
import { MissionReport } from './mission-report/mission-report';
import { MissionReport as MissionReportView } from './mission-report/mission-report.model';
import {
  buildMissionReport,
  readSeenReport,
  writeSeenReport,
} from './mission-report/mission-report.utils';
import { FULL_CAMPAIGN_POPULATION, OVERVIEW_TAB_PARAM } from './overview.constants';
import { FriezeWeek } from './overview.model';
import { buildFrieze, formatSigned } from './overview.utils';
import { OverviewTabs } from './overview-tabs/overview-tabs';
import { OverviewTab, OverviewTabKey } from './overview-tabs/overview-tabs.model';
import {
  buildTabs,
  parseOverviewTab,
  readFavoriteTab,
  writeFavoriteTab,
} from './overview-tabs/overview-tabs.utils';
import { PlanetFigure } from './planet-figure/planet-figure';
import { ScanWires } from './scan-wires/scan-wires';
import { SquadMatches } from './squad-matches/squad-matches';
import { SquadSheet } from './squad-sheet/squad-sheet';
import { SquadRow } from './squad-sheet/squad-sheet.model';
import { buildSquad } from './squad-sheet/squad-sheet.utils';

/**
 * Campaign at a glance: base, frieze, mission, day orders and squad. States, never advice.
 */
@Component({
  selector: 'app-overview',
  imports: [
    LucideDynamicIcon,
    LucideFileText,
    NgOptimizedImage,
    NgTemplateOutlet,
    TranslatePipe,
    PageHeader,
    ResourceState,
    SectionRule,
    CountUp,
    BaseScene,
    PlanetFigure,
    ScanWires,
    ExtractionGauges,
    MissionReadings,
    MissionReport,
    OverviewTabs,
    DayOrders,
    SquadSheet,
    SquadMatches,
    CampaignPanel,
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Overview {
  /**
   * Icon of each concept, for the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Population of a full skyline, the scale the base scene grows on.
   */
  protected readonly fullCampaignPopulation = FULL_CAMPAIGN_POPULATION;

  /**
   * Campaign source, for the campaign and today's haul.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Challenge source, for the daily challenge card.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Ranking source, for the squad's contribution, standings and report honours.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Player source, to name and portray operators.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Translation service, for labels built in code and figure formatting.
   */
  private readonly translation = inject(Translation);

  /**
   * Campaign with its weeks, base and forecast, the page's backbone.
   */
  protected readonly campaignResource = this.campaignApi.campaign;

  /**
   * Today's haul, operator by operator.
   */
  protected readonly todayResource = this.campaignApi.today;

  /**
   * Week's challenges, the source of the daily card.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Weekly ranking, the source of the squad's contribution.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Today's ranking, the source of the squad sheet.
   */
  protected readonly dailyResource = this.rankingApi.daily;

  /**
   * Frozen weeks for the report's titles and ranking; never awaited.
   */
  private readonly historyResource = this.rankingApi.history;

  /**
   * Whether any awaited resource is still loading.
   */
  protected readonly isLoading = anyLoading(
    this.campaignResource,
    this.todayResource,
    this.challengesResource,
    this.rankingResource,
    this.dailyResource,
  );

  /**
   * Whether any awaited resource failed, to offer a retry.
   */
  protected readonly isError = anyError(
    this.campaignResource,
    this.todayResource,
    this.challengesResource,
    this.rankingResource,
    this.dailyResource,
  );

  /**
   * Campaign, `null` while loading, on error or before the first one.
   */
  protected readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  /**
   * Week in progress, `null` outside a running campaign's ten weeks.
   */
  protected readonly currentWeek = computed<CampaignWeek | null>(() => {
    const campaign = this.campaign();
    if (campaign?.status !== 'RUNNING' || campaign.currentWeekIndex === null) {
      return null;
    }
    return campaign.weeks[campaign.currentWeekIndex - 1] ?? null;
  });

  /**
   * Whether a mission can show: running campaign, inside its weeks, with a forecast.
   */
  protected readonly isRunning = computed(
    () => this.currentWeek() !== null && this.campaign()?.forecast !== null,
  );

  /**
   * Base inhabitants, `0` before a base exists.
   */
  protected readonly population = computed(() => this.campaign()?.base?.population ?? 0);

  /**
   * Population this browser last saw, read before the scene records today's.
   */
  private readonly seenPopulation = readSeenPopulation();

  /**
   * Inhabitants gained or lost since the last visit, `0` on a first visit.
   */
  protected readonly sinceLastVisit = computed(() => {
    const population = this.population();
    return this.seenPopulation === null || population === 0 ? 0 : population - this.seenPopulation;
  });

  /**
   * Inhabitants gained or lost over the last replayed day.
   */
  protected readonly populationChange = computed(
    () => this.campaign()?.base?.populationChange ?? 0,
  );

  /**
   * Guardians defeated, one rocket stage each on the scene.
   */
  protected readonly stagesDone = computed(() => this.campaign()?.totals?.guardiansDefeated ?? 0);

  /**
   * Accessible description of the base scene.
   */
  protected readonly sceneLabel = computed(() =>
    this.translation.translate('overview.scene.aria', {
      population: this.format(this.population()),
      stages: this.stagesDone(),
      weeks: CAMPAIGN_WEEK_COUNT,
    }),
  );

  /**
   * Ten-planet frieze, one cell per campaign week.
   */
  protected readonly frieze = computed<readonly FriezeWeek[]>(() =>
    buildFrieze(this.campaign(), (key, params) => this.translation.translate(key, params)),
  );

  /**
   * Week in progress as the mission block shows it, `null` outside one.
   */
  protected readonly mission = computed<Mission | null>(() =>
    buildMission(
      this.campaign(),
      this.currentWeek(),
      resourceValue(this.playersApi.players, []),
      this.translation.language(),
    ),
  );

  /**
   * Accessible description of the mission planet, empty outside a mission.
   */
  protected readonly planetLabel = computed(() => {
    const mission = this.mission();
    if (!mission) {
      return '';
    }
    return this.translation.translate('overview.mission.planetAria', {
      planet: mission.planetName,
      week: mission.weekIndex,
      wounded: this.format(mission.wounded),
      percent: mission.breachPercent,
    });
  });

  /**
   * Whether the Monday report is on screen.
   */
  protected readonly reportOpen = signal(false);

  /**
   * Settled week the report shows, `null` for the last one.
   */
  private readonly reportWeek = signal<number | null>(null);

  /**
   * Last settled week's Monday report, `null` before the first Sunday.
   */
  protected readonly missionReport = computed<MissionReportView | null>(() =>
    this.buildReport(null),
  );

  /**
   * Report on screen: the last one or the week picked on the frieze.
   */
  protected readonly shownReport = computed<MissionReportView | null>(() => {
    const week = this.reportWeek();
    return week === null ? this.missionReport() : this.buildReport(week);
  });

  /**
   * Element focused when the report opened, refocused on close.
   */
  private reportOpener: HTMLElement | null = null;

  /**
   * Sunday's extraction dials, `null` outside a week in progress.
   */
  protected readonly capacity = computed<Capacity | null>(() =>
    buildCapacity(this.campaign(), this.currentWeek()),
  );

  /**
   * What Sunday midnight can still add or take, `null` once the guardian is down.
   */
  protected readonly stakes = computed<SundayStakes | null>(() =>
    buildSundayStakes(this.campaign(), this.currentWeek()),
  );

  /**
   * Guardian's descent over the week and its projected fall.
   */
  protected readonly fall = computed<GuardianFall | null>(() =>
    buildGuardianFall(this.currentWeek(), Date.now()),
  );

  /**
   * Squad's input into the week, one segment per operator.
   */
  protected readonly contribution = computed<Contribution | null>(() =>
    buildContribution(resourceValue(this.rankingResource, null) ?? null, this.currentWeek()),
  );

  /**
   * Daily challenge card, `null` when none was drawn.
   */
  protected readonly dailyRow = computed<BoardRow | null>(() => {
    const language = this.translation.language();
    return buildDailyRow(
      resourceValue(this.challengesResource, null) ?? null,
      this.campaign()?.status === 'RUNNING',
      this.translation.translate('challenges.daily.key'),
      (amount) => formatFigure(amount, language, amount >= 1_000),
      language,
      (key, params) => this.translation.translate(key, params),
    );
  });

  /**
   * Day's haul, `null` outside a week in progress.
   */
  protected readonly tally = computed<DayTally | null>(() =>
    buildTally(
      resourceValue(this.todayResource, null) ?? null,
      this.currentWeek(),
      this.campaign(),
      resourceValue(this.playersApi.players, []),
    ),
  );

  /**
   * Squad sheet rows, with last week's winner flagged.
   */
  protected readonly squad = computed<readonly SquadRow[]>(() =>
    buildSquad(
      resourceValue(this.dailyResource, null) ?? null,
      resourceValue(this.todayResource, null) ?? null,
      resourceValue(this.historyResource, null)?.content[0]?.winnerPlayerId ?? null,
    ),
  );

  /**
   * Router, to keep the selected tab in the URL.
   */
  private readonly router = inject(Router);

  /**
   * Current route, to read and merge the tab query parameter.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * Tab the reader pinned to open the page on, `null` for the default.
   */
  protected readonly favoriteTab = signal<OverviewTabKey | null>(readFavoriteTab());

  /**
   * Tab whose panel shows under the mission.
   */
  protected readonly selectedTab = signal<OverviewTabKey>(
    parseOverviewTab(this.route.snapshot.queryParamMap.get(OVERVIEW_TAB_PARAM)) ??
      this.favoriteTab() ??
      'challenges',
  );

  /**
   * Tab bar entries, the pinned one leading.
   */
  protected readonly tabs = computed<readonly OverviewTab[]>(() =>
    buildTabs((key, params) => this.translation.translate(key, params), this.favoriteTab()),
  );

  /**
   * Players on the roster, the squad sheet's denominator.
   */
  protected readonly rosterCount = computed(
    () => resourceValue(this.dailyResource, null)?.rosterPlayerCount ?? 0,
  );

  /**
   * Whether a base exists (running or closed campaign); otherwise the figure hides, not a zero.
   */
  protected readonly hasBase = computed(() => {
    const key = this.stateKey();
    return key === 'settling' || key === 'closed';
  });

  /**
   * Empty state key when there is no mission to show.
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

  /**
   * Empty state illustration: no campaign yet, or one between missions.
   */
  protected readonly emptyKind = computed<'creation' | 'waiting'>(() =>
    this.stateKey() === 'none' ? 'creation' : 'waiting',
  );

  /**
   * Whether a settled or closed campaign shows its result in place of the mission.
   */
  protected readonly showsSettledResult = computed(
    () => !this.isRunning() && this.hasBase() && !this.isLoading() && !this.isError(),
  );

  /**
   * Empty state by campaign state.
   */
  protected readonly emptyPlate = computed<EmptyPlate>(() => {
    const key = this.stateKey();
    const t = (suffix: string) => this.translation.translate(`overview.state.${key}.${suffix}`);
    return {
      illustration: 'radar',
      eyebrow: t('eyebrow'),
      title: t('title'),
      text: t('text'),
      readouts:
        key === 'none'
          ? [
              { tone: 'live', label: t('ranking'), value: t('rankingValue') },
              { tone: 'todo', label: t('campaign'), value: t('campaignValue') },
            ]
          : [],
    };
  });

  constructor() {
    // Tab in the URL so links reopen it; `replaceUrl` keeps switches off the back button.
    effect(() => {
      const tab = this.selectedTab();
      untracked(() =>
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { [OVERVIEW_TAB_PARAM]: tab },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        }),
      );
    });

    // Auto-opens once per settled week; the context bar's button reopens it.
    effect(() => {
      const report = this.missionReport();
      if (report && readSeenReport() !== report.weekStart) {
        this.reportOpen.set(true);
      }
    });
  }

  /**
   * Reloads every awaited resource after a failure.
   */
  protected retry(): void {
    reloadAll(
      this.campaignResource,
      this.todayResource,
      this.challengesResource,
      this.rankingResource,
      this.dailyResource,
    );
  }

  /**
   * Formats an amount in the active language.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }

  /**
   * Formats a gain or loss with its sign, a true minus for losses.
   */
  protected signed(amount: number): string {
    return formatSigned(amount, (value) => this.format(value));
  }

  /**
   * Opens a settled week's report, the last one when `weekIndex` is `null`.
   */
  protected openReport(weekIndex: number | null = null): void {
    this.reportOpener =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.reportWeek.set(weekIndex);
    this.reportOpen.set(true);
  }

  /**
   * Closes the report; only the last week's counts as seen.
   */
  protected closeReport(): void {
    const report = this.missionReport();
    if (report) {
      writeSeenReport(report.weekStart);
    }
    this.reportOpen.set(false);
    this.reportOpener?.focus();
    this.reportOpener = null;
  }

  /**
   * Pins the tab the page opens on and remembers it in this browser.
   */
  protected pinTab(key: OverviewTabKey | null): void {
    this.favoriteTab.set(key);
    writeFavoriteTab(key);
  }

  /**
   * Builds a settled week's report, the last one when `weekIndex` is `null`.
   */
  private buildReport(weekIndex: number | null): MissionReportView | null {
    return buildMissionReport(
      this.campaign(),
      resourceValue(this.playersApi.players, []),
      resourceValue(this.historyResource, null)?.content ?? [],
      this.translation.language(),
      (key, params) => this.translation.translate(key, params),
      weekIndex,
    );
  }
}
