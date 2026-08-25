import { computed, inject, Service, Signal } from '@angular/core';

import { BossApi } from '@core/boss/boss-api';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Translation } from '@core/i18n/translation';
import { ChartBar } from '@shared/chart/chart.model';
import { ColonyApi } from './colony-api';
import {
  formatGauge,
  formatPercentage,
  formatPopulation,
  formatSignedGauge,
} from './colony-format.utils';
import { Colony, ColonyBuildingTier, ColonyGauge, ColonyRunHistory } from './colony.model';
import {
  ColonyBossState,
  ColonyBossView,
  ColonyBuildingState,
  ColonyBuildingView,
  ColonyGaugeView,
  ColonyRunView,
  RunDayParts,
} from './colony-view.model';

/**
 * Tailwind utilities each gauge is drawn in, in its ordinary state.
 *
 * Food green and Energy blue: both come from the application's existing accent set, and neither is
 * the brand amber the population itself is drawn in, so a gauge is never mistaken for the figure it
 * feeds.
 */
const GAUGE_COLORS: Record<ColonyGauge, { readonly fill: string; readonly text: string }> = {
  FOOD: {
    fill: 'bg-accent-green',
    text: 'text-accent-green',
  },
  ENERGY: {
    fill: 'bg-accent-blue',
    text: 'text-accent-blue',
  },
};

/**
 * Tailwind utilities both gauges fall back to while the colony is in distress.
 *
 * The desaturated `danger` red rather than `accent-red`, which marks boss damage everywhere else:
 * a colony in trouble is a state, not a hit taken.
 */
const DISTRESS_COLORS = {
  fill: 'bg-danger',
  text: 'text-danger',
};

/**
 * The squad's colony, resolved once into everything the page lays out.
 *
 * Every label a view model carries is baked here, already translated and already formatted, so the
 * page only positions them — the same arrangement `BossCampaign` uses for the campaign.
 *
 * Provided at component level rather than in the injector root; the underlying `httpResource`s live
 * in the api services and are shared regardless.
 */
@Service()
export class ColonyView {
  /**
   * Data-access service backing the colony, its curve and its history.
   */
  private readonly colonyApi = inject(ColonyApi);

  /**
   * Data-access service backing the run's fights, which the boss row reads.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * i18n service resolving every translated label baked into a view model.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resources this view reads.
   */
  private readonly colonyResource = this.colonyApi.colony;
  private readonly trajectoryResource = this.colonyApi.trajectory;
  private readonly historyResource = this.colonyApi.history;
  private readonly bossHistoryResource = this.bossApi.history;
  private readonly currentBossResource = this.bossApi.current;

  /**
   * Whether any backing resource is still loading.
   */
  public readonly isLoading = anyLoading(
    this.colonyResource,
    this.trajectoryResource,
    this.historyResource,
    this.bossHistoryResource,
    this.currentBossResource,
  );

  /**
   * Whether any backing resource failed to load.
   */
  public readonly hasError = anyError(
    this.colonyResource,
    this.trajectoryResource,
    this.historyResource,
    this.bossHistoryResource,
    this.currentBossResource,
  );

  /**
   * The colony itself, or `null` while it has not resolved.
   */
  public readonly colony: Signal<Colony | null> = computed(
    () => resourceValue(this.colonyResource, null) ?? null,
  );

  /**
   * The run's population curve, or `null` while it has not resolved.
   */
  private readonly trajectory = computed(
    () => resourceValue(this.trajectoryResource, null) ?? null,
  );

  /**
   * Context line above the page title: the section, the run and the week inside it.
   */
  public readonly eyebrow = computed<string>(() => {
    const colony = this.colony();
    if (colony === null) {
      return this.translation.translate('campaign.title');
    }

    return this.translation.translate('colony.eyebrow', {
      run: colony.runNumber,
      week: colony.runWeekIndex,
      weeks: colony.runWeekCount,
    });
  });

  /**
   * Trailing label of the context bar: how far into the run today is. Split into its parts so the
   * bar can set the day itself apart from the word and the total.
   */
  public readonly runDayParts = computed<RunDayParts | null>(() => {
    const colony = this.colony();

    return colony === null
      ? null
      : {
          word: this.translation.translate('colony.runDayWord'),
          day: colony.runDay,
          days: colony.runDayCount,
          hint: this.translation.translate('colony.runDayHint', { days: colony.runDayCount }),
        };
  });

  /**
   * Already-formatted headline figures of the population block.
   */
  public readonly populationLabel = computed<string>(() =>
    this.population((colony) => colony.population),
  );
  public readonly capacityLabel = computed<string>(() =>
    this.population((colony) => colony.capacity),
  );

  /**
   * Materials banked, the run's one spendable figure.
   */
  public readonly materialsLabel = computed<string>(() =>
    this.population((colony) => colony.materials),
  );

  /**
   * What the tier being worked towards costs, so the materials figure can be read against the bar
   * it is filling rather than as a bare total. Empty once the last tier is up and materials buy
   * nothing more.
   */
  public readonly nextTierThresholdLabel = computed<string>(() => {
    const nextTier = this.colony()?.nextTier;

    return nextTier === null || nextTier === undefined
      ? ''
      : formatPopulation(nextTier.materialsThreshold, this.translation.language());
  });

  /**
   * Health, as a whole percentage. Fractions of a point would suggest a precision the geometric
   * mean of two moving gauges does not have.
   */
  public readonly healthLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null ? '' : `${Math.round(colony.healthPercentage)}`;
  });

  /**
   * What health is, in one sentence: the mean it comes from. The band draws it as a bar among the
   * two it is derived from, and nothing there says it is never fed directly.
   */
  public readonly healthDescription = computed<string>(() =>
    this.translation.translate('colony.healthDescription'),
  );

  /**
   * What the current capacity comes from, so the figure beside the population is attributable.
   */
  public readonly capacitySourceLabel = computed<string>(() => {
    const colony = this.colony();
    if (colony === null) {
      return '';
    }

    const highest = [...colony.buildings].reverse().find((tier) => tier.erected);

    return this.translation.translate('colony.capacitySource', {
      building: this.translation.translate(`colony.building.${highest?.building ?? 'CAMP'}`),
    });
  });

  /**
   * The tier being worked towards, named.
   */
  public readonly nextTierLabel = computed<string>(() => {
    const nextTier = this.colony()?.nextTier;

    return nextTier == null
      ? ''
      : this.translation.translate('colony.nextTier', {
          building: this.translation.translate(`colony.building.${nextTier.building}`),
        });
  });

  /**
   * Progress towards that tier, as a percentage.
   */
  public readonly nextTierPercentageLabel = computed<string>(() => {
    const nextTier = this.colony()?.nextTier;

    return nextTier == null
      ? ''
      : `${formatPercentage(nextTier.progressPercentage, this.translation.language())} %`;
  });

  /**
   * Materials gathered against materials needed, and what is still missing.
   */
  public readonly nextTierProgressLabel = computed<string>(() => {
    const colony = this.colony();
    if (colony === null || colony.nextTier === null) {
      return '';
    }

    const language = this.translation.language();

    return this.translation.translate('colony.nextTierProgress', {
      materials: formatPopulation(colony.materials, language),
      threshold: formatPopulation(colony.nextTier.materialsThreshold, language),
      missing: formatPopulation(colony.nextTier.missingMaterials, language),
    });
  });

  /**
   * The two gauges, Food first.
   */
  public readonly gauges = computed<readonly ColonyGaugeView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    return [
      this.toGaugeView('FOOD', colony.food, colony),
      this.toGaugeView('ENERGY', colony.energy, colony),
    ];
  });

  /**
   * Every building tier, cheapest first.
   */
  public readonly buildings = computed<readonly ColonyBuildingView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    return colony.buildings.map((tier) => this.toBuildingView(tier, colony));
  });

  /**
   * Buildings earned over the three a run can put up.
   *
   * The starting camp is excluded from both terms: it costs nothing and is there from day one, so
   * counting it would make every run open on `1/4`.
   */
  public readonly buildingsProgressLabel = computed<string>(() => {
    const colony = this.colony();
    if (colony === null) {
      return '';
    }

    const earned = colony.buildings.filter(
      (tier) => tier.erected && tier.materialsThreshold > 0,
    ).length;
    const total = colony.buildings.filter((tier) => tier.materialsThreshold > 0).length;

    return `${earned}/${total}`;
  });

  /**
   * The run's ten fights, oldest first.
   */
  public readonly bosses = computed<readonly ColonyBossView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    // The history endpoint is run-scoped and ordered most recent first, so reversing it yields
    // exactly this run's finalized weeks in order: week one is its first element.
    const finalized = [...(resourceValue(this.bossHistoryResource, null)?.content ?? [])].reverse();

    return Array.from({ length: colony.bossCount }, (_, index) =>
      this.toBossView(index + 1, colony, finalized[index]?.defeated),
    );
  });

  /**
   * The population curve, one bar per day played.
   */
  public readonly curve = computed<readonly ChartBar[]>(() => {
    const trajectory = this.trajectory();
    if (trajectory === null) {
      return [];
    }

    const language = this.translation.language();

    return trajectory.points.map((point) => ({
      label: this.translation.translate('colony.curve.day', { day: point.runDay }),
      value: point.population,
      detail: this.translation.translate('colony.curve.detail', {
        capacity: formatPopulation(point.capacity, language),
        players: point.activePlayerCount,
      }),
      highlighted: point.population === trajectory.peakPopulation,
      muted: false,
    }));
  });

  /**
   * Caption beside the curve: the run's peak and its average.
   */
  public readonly curveCaption = computed<string>(() => {
    const trajectory = this.trajectory();
    if (trajectory === null) {
      return '';
    }

    const language = this.translation.language();

    return this.translation.translate('colony.curve.caption', {
      peak: formatPopulation(trajectory.peakPopulation, language),
      average: formatPopulation(trajectory.averagePopulation, language),
    });
  });

  /**
   * Days buildings went up, spelled out under the curve so it can be read against them.
   */
  public readonly milestoneLabels = computed<readonly string[]>(() => {
    const trajectory = this.trajectory();

    return (trajectory?.milestones ?? []).map((milestone) =>
      this.translation.translate('colony.curve.milestone', {
        building: this.translation.translate(`colony.building.${milestone.building}`),
        day: milestone.runDay,
      }),
    );
  });

  /**
   * Every run, the one in progress first, then the closed ones from the latest to the first.
   */
  public readonly runs = computed<readonly ColonyRunView[]>(() => {
    const closed = resourceValue(this.historyResource, []).map((run) => this.toClosedRunView(run));
    const current = this.currentRunView();

    return current === null ? closed : [current, ...closed];
  });

  /**
   * Reloads every backing resource after a failure.
   */
  public reload(): void {
    reloadAll(
      this.colonyResource,
      this.trajectoryResource,
      this.historyResource,
      this.bossHistoryResource,
      this.currentBossResource,
    );
  }

  /**
   * Formats one figure of the colony, or returns nothing while it has not resolved.
   *
   * @param pick - Which figure to read.
   * @returns The grouped figure.
   */
  private population(pick: (colony: Colony) => number): string {
    const colony = this.colony();

    return colony === null ? '' : formatPopulation(pick(colony), this.translation.language());
  }

  /**
   * Resolves one gauge into its view model.
   *
   * @param gauge - Which gauge this is.
   * @param state - Its value and the day's movement on it.
   * @param colony - The colony it belongs to.
   * @returns The display-ready gauge.
   */
  private toGaugeView(gauge: ColonyGauge, state: Colony['food'], colony: Colony): ColonyGaugeView {
    const language = this.translation.language();
    const colors = colony.alert ? DISTRESS_COLORS : GAUGE_COLORS[gauge];
    const label = this.translation.translate(`colony.gauge.${gauge}.name`);
    const valueLabel = formatGauge(state.value, language);

    return {
      gauge,
      initial: label.charAt(0),
      label,
      percentage: state.value,
      valueLabel,
      // The three fragments the sentence is made of are separate keys rather than one block of copy
      // per gauge: what feeds a gauge and how it is played are two different answers, and the day's
      // movement is a figure that has to be interpolated anyway.
      descriptionLabel: this.translation.translate('colony.gauge.description', {
        detail: this.translation.translate(`colony.gauge.${gauge}.detail`),
        rule: this.translation.translate(`colony.gauge.${gauge}.rule`),
        movement: this.translation.translate('colony.gauge.movement', {
          gain: formatSignedGauge(state.gain, language),
          loss: formatSignedGauge(-state.loss, language),
        }),
      }),
      fillClass: colors.fill,
      textClass: colors.text,
      isLimiting: colony.limitingGauge === gauge,
    };
  }

  /**
   * Resolves one building tier into its view model.
   *
   * @param tier - The tier.
   * @param colony - The colony it belongs to.
   * @returns The display-ready tier.
   */
  private toBuildingView(tier: ColonyBuildingTier, colony: Colony): ColonyBuildingView {
    const language = this.translation.language();
    const isNext = colony.nextTier?.building === tier.building;
    const state: ColonyBuildingState = tier.erected ? 'erected' : isNext ? 'next' : 'locked';

    return {
      building: tier.building,
      name: this.translation.translate(`colony.building.${tier.building}`),
      state,
      capacityLabel: formatPopulation(tier.capacity, language),
      detailLabel: this.buildingDetail(tier, colony, language),
    };
  }

  /**
   * Builds one tier's sub-line: what it cost and when it went up, or what is still missing.
   *
   * @param tier - The tier.
   * @param colony - The colony it belongs to.
   * @param language - Active language.
   * @returns The already-translated sub-line.
   */
  private buildingDetail(tier: ColonyBuildingTier, colony: Colony, language: 'fr' | 'en'): string {
    const materials = formatPopulation(tier.materialsThreshold, language);

    if (tier.erected) {
      return this.translation.translate('colony.building.erected', {
        materials,
        day: tier.erectedOnRunDay ?? 1,
      });
    }

    return this.translation.translate('colony.building.missing', {
      materials,
      missing: formatPopulation(tier.materialsThreshold - colony.materials, language),
    });
  }

  /**
   * Resolves one week of the boss row into its view model.
   *
   * @param weekIndex - Week of the run, from one.
   * @param colony - The colony the run belongs to.
   * @param defeated - Whether that week's fight was won, `undefined` while it is not settled.
   * @returns The display-ready fight.
   */
  private toBossView(
    weekIndex: number,
    colony: Colony,
    defeated: boolean | undefined,
  ): ColonyBossView {
    const state: ColonyBossState =
      defeated === undefined
        ? weekIndex === colony.runWeekIndex
          ? 'current'
          : 'upcoming'
        : defeated
          ? 'defeated'
          : 'survived';

    return {
      weekIndex,
      state,
      label: this.translation.translate('colony.boss.week', {
        week: weekIndex,
        status: this.translation.translate(`colony.boss.status.${state}`),
      }),
      materialsLabel: this.translation.translate('colony.boss.materials', {
        materials: colony.materialsPerBoss,
      }),
      materialsEarned: state === 'defeated',
    };
  }

  /**
   * Resolves the run in progress into a history row, so the table opens on it.
   *
   * @returns The display-ready run, or `null` while the colony has not resolved.
   */
  private currentRunView(): ColonyRunView | null {
    const colony = this.colony();
    if (colony === null) {
      return null;
    }

    const language = this.translation.language();
    const earned = colony.buildings.filter(
      (tier) => tier.erected && tier.materialsThreshold > 0,
    ).length;
    const total = colony.buildings.filter((tier) => tier.materialsThreshold > 0).length;

    return {
      runNumber: colony.runNumber,
      label: this.translation.translate('colony.history.run', { run: colony.runNumber }),
      isCurrent: true,
      finalLabel: formatPopulation(colony.population, language),
      buildingsLabel: `${earned}/${total}`,
      bossesLabel: `${colony.defeatedBosses}/${colony.bossCount}`,
    };
  }

  /**
   * Resolves one closed run into a history row.
   *
   * @param run - The closed run.
   * @returns The display-ready run.
   */
  private toClosedRunView(run: ColonyRunHistory): ColonyRunView {
    const language = this.translation.language();

    return {
      runNumber: run.runNumber,
      label: this.translation.translate('colony.history.run', { run: run.runNumber }),
      isCurrent: false,
      finalLabel: formatPopulation(run.finalPopulation, language),
      // The free starting camp is excluded from both terms, as it is everywhere else on the page.
      buildingsLabel: `${run.erectedBuildings - 1}/${run.buildingCount - 1}`,
      bossesLabel: `${run.defeatedBosses}/${run.bossCount}`,
    };
  }
}
