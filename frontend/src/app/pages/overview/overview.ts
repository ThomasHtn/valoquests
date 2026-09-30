import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { LucideFileText } from '@lucide/angular';
import { CampaignApi } from '@core/campaign/campaign-api';
import { CAMPAIGN_WEEK_COUNT, CampaignWeek } from '@core/campaign/campaign.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolveLocale } from '@core/i18n/locale.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { PageHeader } from '@layout/page-header/page-header';
import { CountUp } from '@shared/count-up/count-up';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionRule } from '@shared/section-rule/section-rule';
import { BoardRow } from '../challenges/challenges.model';
import { formatFigure } from '../leaderboard/leaderboard-board.utils';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { BaseScene } from './base-scene/base-scene';
import { CampaignPanel } from './campaign-panel/campaign-panel';
import { DayOrders } from './day-orders/day-orders';
import { ExtractionGauges } from './extraction-gauges/extraction-gauges';
import { MissionReadings } from './mission-readings/mission-readings';
import { MissionReport } from './mission-report/mission-report';
import { OverviewTabs } from './overview-tabs/overview-tabs';
import {
  Capacity,
  Contribution,
  DayTally,
  FriezeWeek,
  Mission,
  MissionReport as MissionReportView,
  OverviewTab,
  OverviewTabKey,
  SquadRow,
} from './overview.model';
import {
  buildCapacity,
  buildContribution,
  buildDailyRow,
  buildFrieze,
  buildMission,
  buildMissionReport,
  buildSquad,
  buildTabs,
  buildTally,
} from './overview.utils';
import { PlanetFigure } from './planet-figure/planet-figure';
import { ScanWires } from './scan-wires';
import { SquadMatches } from './squad-matches/squad-matches';
import { SquadSheet } from './squad-sheet/squad-sheet';
import { FULL_CAMPAIGN_POPULATION } from './overview.constants';
import {
  readFavoriteTab,
  readSeenReport,
  writeFavoriteTab,
  writeSeenReport,
} from './overview.utils';

/**
 * The state of the campaign, at a glance: the base and its rocket, the ten weeks, the mission of
 * the week, the orders of the day and what each operator brought in today.
 *
 * A screen of states, never of advice: each figure is doubled by what it pays for in people, and
 * the rule of Sunday's settlement belongs to the rules page.
 */
@Component({
  selector: 'app-overview',
  imports: [
    LucideFileText,
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
  protected readonly fullCampaignPopulation = FULL_CAMPAIGN_POPULATION;

  private readonly campaignApi = inject(CampaignApi);

  private readonly challengesApi = inject(ChallengesApi);

  private readonly rankingApi = inject(RankingApi);

  private readonly playersApi = inject(PlayersApi);

  private readonly translation = inject(Translation);

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly todayResource = this.campaignApi.today;

  protected readonly challengesResource = this.challengesApi.current;

  protected readonly rankingResource = this.rankingApi.current;

  protected readonly dailyResource = this.rankingApi.daily;

  /**
   * The frozen weeks, for the report's titles and ranking. Never awaited: the report reads what
   * it finds.
   */
  private readonly historyResource = this.rankingApi.history;

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

  protected readonly frieze = computed<readonly FriezeWeek[]>(() =>
    buildFrieze(this.campaign(), (key, params) => this.translation.translate(key, params)),
  );

  protected readonly mission = computed<Mission | null>(() =>
    buildMission(
      this.campaign(),
      this.currentWeek(),
      resourceValue(this.playersApi.players, []),
      this.translation.language(),
    ),
  );

  /**
   * Whether the Monday report is on screen.
   */
  protected readonly reportOpen = signal(false);

  /**
   * The settled week the report shows: `null` for the last one, or a week picked on the frieze.
   */
  private readonly reportWeek = signal<number | null>(null);

  /**
   * The last settled week, told as the Monday report; `null` before the first Sunday.
   */
  protected readonly missionReport = computed<MissionReportView | null>(() =>
    this.buildReport(null),
  );

  /**
   * The report on screen: the last settled week's, or the one picked on the frieze.
   */
  protected readonly shownReport = computed<MissionReportView | null>(() => {
    const week = this.reportWeek();
    return week === null ? this.missionReport() : this.buildReport(week);
  });

  /**
   * What had focus when the report opened, to hand it back on close.
   */
  private reportOpener: HTMLElement | null = null;

  protected readonly capacity = computed<Capacity | null>(() =>
    buildCapacity(this.campaign(), this.currentWeek()),
  );

  /**
   * What the squad has put into the week, one segment per operator.
   */
  protected readonly contribution = computed<Contribution | null>(() =>
    buildContribution(resourceValue(this.rankingResource, null) ?? null, this.currentWeek()),
  );

  protected readonly dailyRow = computed<BoardRow | null>(() => {
    const locale = resolveLocale(this.translation.language());
    return buildDailyRow(
      resourceValue(this.challengesResource, null) ?? null,
      this.campaign()?.status === 'RUNNING',
      this.translation.translate('challenges.daily.key'),
      (amount) => formatFigure(amount, locale, amount >= 1_000),
      locale,
      (key, params) => this.translation.translate(key, params),
    );
  });

  protected readonly tally = computed<DayTally | null>(() =>
    buildTally(
      resourceValue(this.todayResource, null) ?? null,
      this.currentWeek(),
      this.campaign(),
      resourceValue(this.playersApi.players, []),
    ),
  );

  protected readonly squad = computed<readonly SquadRow[]>(() =>
    buildSquad(
      resourceValue(this.dailyResource, null) ?? null,
      resourceValue(this.todayResource, null) ?? null,
      resourceValue(this.historyResource, null)?.content[0]?.winnerPlayerId ?? null,
    ),
  );

  /**
   * The tab the reader pinned to open the page on, or `null` for the first one.
   */
  protected readonly favoriteTab = signal<OverviewTabKey | null>(readFavoriteTab());

  /**
   * The tab whose panel is on screen under the mission.
   */
  protected readonly selectedTab = signal<OverviewTabKey>(this.favoriteTab() ?? 'challenges');

  protected readonly tabs = computed<readonly OverviewTab[]>(() =>
    buildTabs((key, params) => this.translation.translate(key, params), this.favoriteTab()),
  );

  protected readonly rosterCount = computed(
    () => resourceValue(this.dailyResource, null)?.rosterPlayerCount ?? 0,
  );

  protected readonly playerCount = computed(
    () => resourceValue(this.dailyResource, null)?.ranking.length ?? 0,
  );

  /**
   * Whether a base exists to be counted: only a running or closed campaign has one. Before that,
   * the scene stays but the figure is hidden rather than reading a population of zero.
   */
  protected readonly hasBase = computed(() => {
    const key = this.stateKey();
    return key === 'settling' || key === 'closed';
  });

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

  /**
   * The empty state, by campaign state: what the squad waits on, and that the ranking already runs.
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
    // The report opens on its own once per settled week, the first time the page is opened after
    // Sunday; the context bar's button brings it back afterwards.
    effect(() => {
      const report = this.missionReport();
      if (report && readSeenReport() !== report.weekStart) {
        this.reportOpen.set(true);
      }
    });
  }

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

  /**
   * Opens a settled week's report: the last one from the context bar, any one from the frieze.
   */
  protected openReport(weekIndex: number | null = null): void {
    this.reportOpener =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.reportWeek.set(weekIndex);
    this.reportOpen.set(true);
  }

  /**
   * Closes the report. Only the last week's counts as seen: an older one read again changes nothing.
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

  protected pinTab(key: OverviewTabKey | null): void {
    this.favoriteTab.set(key);
    writeFavoriteTab(key);
  }

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
