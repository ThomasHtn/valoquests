import { computed, inject, Service, Signal } from '@angular/core';

import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Translation } from '@core/i18n/translation';
import { ChartBar } from '@shared/chart/chart.model';
import { ColonyApi } from './colony-api';
import {
  formatEfficiencyGain,
  formatGauge,
  formatMultiplier,
  formatPopulation,
  formatSignedPopulation,
} from './colony-format.utils';
import {
  FOOD_SEGMENT_EMPTY_COLOR,
  FOOD_SEGMENT_LAST_DAY_COLOR,
  FOOD_SEGMENT_TODAY_COLOR,
  foodSegmentPlayedColor,
  PRESENCE_PIP_FULL_CLASS,
  PRESENCE_PIP_PARTIAL_CLASS,
} from './colony-gauge.utils';
import { tierGlyphFor } from './colony-tier.utils';
import {
  Colony,
  ColonyPresencePlayer,
  ColonyRunHistory,
  ColonyTier,
  ColonyWeek,
} from './colony.model';
import {
  ColonyAttractivityView,
  ColonyBatteryView,
  ColonyBossView,
  ColonyDeltaView,
  ColonyFoodDayView,
  ColonyFoodRingView,
  ColonyPresencePipView,
  ColonyRunView,
  ColonyTierStepView,
  RunDayParts,
} from './colony-view.model';

/**
 * Letters of a player's name kept for a turnout pip.
 *
 * Three: enough to tell a squad of seven apart, short enough that seven of them fit on one line of
 * the hover card without wrapping.
 */
const INITIALS_LENGTH = 3;

/**
 * Morale a surviving boss costs, mirrored from `DefaultColonyRuleset#moraleForSurvivingBoss`.
 *
 * Exactly what an elite win pays, which is the invariant the morale table is built on.
 */
const MORALE_FOR_SURVIVING_BOSS = -7;

/**
 * Locales the weekday names of the food strip are resolved in.
 *
 * Only ever spoken, never drawn: the pills are too short to carry a letter, so the day each one
 * stands for reaches a reader through its accessible name alone.
 */
const WEEKDAY_LOCALES: Record<'fr' | 'en', string> = { fr: 'fr-FR', en: 'en-US' };

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
   * i18n service resolving every translated label baked into a view model.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resources this view reads.
   */
  private readonly colonyResource = this.colonyApi.colony;
  private readonly trajectoryResource = this.colonyApi.trajectory;
  private readonly historyResource = this.colonyApi.history;

  /**
   * Whether any backing resource is still loading.
   */
  public readonly isLoading = anyLoading(
    this.colonyResource,
    this.trajectoryResource,
    this.historyResource,
  );

  /**
   * Whether any backing resource failed to load.
   */
  public readonly hasError = anyError(
    this.colonyResource,
    this.trajectoryResource,
    this.historyResource,
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
    this.grouped((colony) => colony.population),
  );

  /**
   * Materials banked, the currency the ladder is priced in.
   */
  public readonly materialsLabel = computed<string>(() =>
    this.grouped((colony) => colony.materials),
  );

  /**
   * What a boss still standing costs the colony, for the fight under way's hover card — a fixed
   * penalty, so it holds regardless of which week is asking.
   *
   * Mirrors `DefaultColonyRuleset#moraleForSurvivingBoss`.
   */
  public readonly defeatMoraleLabel = computed<string>(() =>
    formatSignedPopulation(MORALE_FOR_SURVIVING_BOSS, this.translation.language()),
  );

  /**
   * What this week has already secured and Monday will credit, or `null` when nothing has been.
   *
   * The banked total only ever moves on a Monday, so it reads as a flat zero for the whole first week
   * of a run while the squad is in fact earning. This is the figure that says so, and it is dropped
   * entirely rather than shown as `+0`: a zero here would state that nothing is coming, which on a
   * Tuesday is a different claim from saying nothing at all.
   */
  public readonly pendingMaterialsLabel = computed<string | null>(() => {
    const pending = this.colony()?.pendingMaterials ?? 0;

    return pending <= 0
      ? null
      : this.translation.translate('colony.materialsPending', {
          materials: formatPopulation(pending, this.translation.language()),
        });
  });

  /**
   * Share of what the food can feed the population already fills, which is how high the hexagon is
   * filled.
   *
   * There is only one ceiling now, so there is no denominator to choose: the town climbs towards what
   * its food feeds and the hexagon reads against that. Still clamped, since the population trails the
   * ceiling by a night and a sharp drop in food can briefly put it above.
   */
  public readonly populationPercentage = computed<number>(() => {
    const colony = this.colony();

    return colony === null ? 0 : this.percentageOf(colony.population, colony.feedablePopulation);
  });

  /**
   * What the night moved, and which way.
   */
  public readonly delta = computed<ColonyDeltaView | null>(() => {
    const colony = this.colony();
    if (colony === null) {
      return null;
    }

    return {
      label: formatSignedPopulation(colony.populationChange, this.translation.language()),
      isPositive: colony.populationChange > 0,
      isNegative: colony.populationChange < 0,
    };
  });

  /**
   * Accessible name of the population hexagon.
   *
   * Says exactly what the shape says: the figure, the ceiling it is filling towards — a full
   * hexagon *is* the food ceiling reached, which is why there is no mark drawn on it — what the night
   * moved, and what pressing it opens. All four are carried by the fill, the silhouette and the
   * raised exponent, and none of them in text, so this is where a reader without the shapes gets
   * them.
   */
  public readonly hexagonAriaLabel = computed<string>(() => {
    const colony = this.colony();
    if (colony === null) {
      return this.translation.translate('colony.curve.open');
    }

    const language = this.translation.language();

    return this.translation.translate('colony.hexagonAria', {
      population: formatPopulation(colony.population, language),
      change: formatSignedPopulation(colony.populationChange, language),
      ceiling: formatPopulation(colony.feedablePopulation, language),
    });
  });

  /**
   * Fights won over the ten a run holds.
   *
   * The denominator is the whole run, never the weeks elapsed: `2 / 3` on a Monday of week four
   * reads as a total, and a total that moves every week is not a total.
   */
  public readonly bossesLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null ? '' : `${colony.defeatedBosses} / ${colony.bossCount}`;
  });

  /**
   * The turnout battery: one cell per roster member, lit by tonight's head count.
   */
  public readonly battery = computed<ColonyBatteryView | null>(() => {
    const colony = this.colony();

    return colony === null ? null : this.batteryView(colony);
  });

  /**
   * The morale bar, read as what it buys: tonight's arrivals.
   */
  public readonly attractivity = computed<ColonyAttractivityView | null>(() => {
    const colony = this.colony();

    return colony === null ? null : this.attractivityView(colony);
  });

  /**
   * The food ring: the week's seven evenings around the sum they add up to.
   */
  public readonly foodRing = computed<ColonyFoodRingView | null>(() => {
    const colony = this.colony();

    return colony === null ? null : this.foodRingView(colony);
  });

  /**
   * One pip per player of the roster, lit by how far into today they got.
   *
   * The unlit pips are the social engine of the whole feature: the first game of everybody's evening
   * is the most valuable one of the week, and this is where that shows.
   */
  public readonly presencePips = computed<readonly ColonyPresencePipView[]>(() => {
    const players = this.colony()?.presence.players ?? [];

    return players.map((player) => this.toPip(player));
  });

  /**
   * The food window's `foodWindowDays` slots, oldest first — the food rail's own track, and the
   * ring the resource band draws it in.
   *
   * The counterpart of {@link presencePips} on the other rail: the stock is a rolling window, so its
   * days are what say *when* the squad played. Each played day is drawn against the best day of the
   * window, which puts them on one scale without inventing a ceiling the stock does not have.
   *
   * Always `foodWindowDays` slots, never just what the window holds: a run still filling its first
   * week gets its lived days padded out with {@link ColonyFoodDayView.isPlaceholder} slots trailing
   * after the most recent one, so every pod keeps the same size around the ring from day one rather
   * than the few lived days stretching to fill the whole ring, and the sweep still reads oldest to
   * newest to not-lived-yet, left to right.
   */
  public readonly foodDays = computed<readonly ColonyFoodDayView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    const best = this.bestHarvest(colony);
    const language = this.translation.language();

    // Oldest first on the wire, kept in that order: the most recent day — the one still open,
    // closing at tonight's reset — trails the sweep rather than leading it.
    const lived = colony.foodWindow.map((entry, index, array) => {
      const percentage = this.percentageOf(entry.harvest, best);
      const isLast = index === array.length - 1;
      const isToday = entry.day === colony.day;

      return {
        day: entry.day,
        percentage,
        isLast,
        isToday,
        isPlaceholder: false,
        segmentColor: isLast
          ? FOOD_SEGMENT_LAST_DAY_COLOR
          : isToday
            ? FOOD_SEGMENT_TODAY_COLOR
            : percentage > 0
              ? foodSegmentPlayedColor(percentage)
              : FOOD_SEGMENT_EMPTY_COLOR,
        ariaLabel: this.translation.translate('colony.track.food.day', {
          day: this.weekdayName(entry.day),
          food: formatPopulation(entry.harvest, language),
        }),
      };
    });

    const placeholderCount = Math.max(0, colony.foodWindowDays - lived.length);
    const placeholders = Array.from({ length: placeholderCount }, (_, index) => ({
      day: `placeholder-${index}`,
      percentage: 0,
      isLast: false,
      isToday: false,
      isPlaceholder: true,
      segmentColor: FOOD_SEGMENT_EMPTY_COLOR,
      ariaLabel: this.translation.translate('colony.track.food.dayUnplayed'),
    }));

    return [...lived, ...placeholders];
  });

  /**
   * The steps of the ladder around the town's own, lowest first.
   */
  public readonly ladder = computed<readonly ColonyTierStepView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    // The ladder is ordered lowest step first, so the step being climbed is the one right after the
    // town's own. Found by position rather than by comparing thresholds with `nextTier`: the two are
    // the same step, but one is a float the other computes again.
    const currentIndex = colony.ladder.findIndex((tier) => tier.state === 'CURRENT');

    return colony.ladder.map((tier, index) =>
      this.toTierStep(tier, colony, currentIndex >= 0 && index === currentIndex + 1),
    );
  });

  /**
   * The run's ten fights, oldest first.
   */
  public readonly bosses = computed<readonly ColonyBossView[]>(() => {
    const colony = this.colony();

    return (colony?.weeks ?? []).map((week) => this.toBossView(week));
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
        feedable: formatPopulation(point.feedablePopulation, language),
        efficiency: formatGauge(point.efficiency, language),
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
   * Days the town changed name, spelled out under the curve so a step in it reads as housing rather
   * than as a good week.
   */
  public readonly milestoneLabels = computed<readonly string[]>(() => {
    const trajectory = this.trajectory();

    return (trajectory?.milestones ?? []).map((milestone) =>
      this.translation.translate('colony.curve.milestone', {
        tier: this.tierName(milestone),
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
    reloadAll(this.colonyResource, this.trajectoryResource, this.historyResource);
  }

  /**
   * The food ring: the week's seven evenings, and the small sum they add up to.
   *
   * The headline used to be the raw stock beside the efficiency as a bare factor, `440 ×8,00`, which
   * read as a multiplication with no operands. It is the weekly surplus instead — what the harvest
   * nets once the town has eaten, signed and already the answer to "is tonight worth playing" — with
   * what it came from written above it: `récolte − consommation = surplus`, at the ring's own centre
   * rather than packed into a symbol beside a total.
   *
   * @param colony - The colony.
   * @returns The display-ready ring.
   */
  private foodRingView(colony: Colony): ColonyFoodRingView {
    const language = this.translation.language();

    return {
      days: this.foodDays(),
      consumptionLabel: formatPopulation(colony.weeklyConsumption, language),
      surplusLabel: formatSignedPopulation(colony.weeklySurplus, language),
      efficiencyLabel: this.translation.translate('colony.track.food.efficiencyValue', {
        inhabitants: formatGauge(colony.efficiency, language),
      }),
      efficiencyFactorLabel: formatMultiplier(colony.efficiency, language),
      ariaLabel: this.translation.translate('colony.track.food.aria', {
        stock: formatPopulation(colony.foodStock, language),
        days: colony.foodWindowDays,
        efficiency: formatGauge(colony.efficiency, language),
        feedable: formatPopulation(colony.feedablePopulation, language),
      }),
      description: this.translation.translate('colony.track.food.description'),
      purpose: this.translation.translate('colony.track.food.note'),
    };
  }

  /**
   * Returns the biggest harvest of the seven-day window, which the rail is read against.
   *
   * @param colony - The colony.
   * @returns The best day's harvest, zero on a week nobody played.
   */
  private bestHarvest(colony: Colony): number {
    return colony.foodWindow.reduce((best, day) => Math.max(best, day.harvest), 0);
  }

  /**
   * Names one weekday in the active language.
   *
   * `Intl` rather than a translated list of seven: the dictionaries would carry fourteen entries for
   * something the platform already knows in every locale.
   *
   * @param isoDay - The day, as `YYYY-MM-DD`.
   * @returns The weekday, spelled out.
   */
  private weekdayName(isoDay: string): string {
    return new Intl.DateTimeFormat(WEEKDAY_LOCALES[this.translation.language()], {
      weekday: 'long',
    }).format(new Date(`${isoDay}T00:00:00Z`));
  }

  /**
   * The turnout battery: a cell per roster member, lit bottom-up by tonight's head count.
   *
   * Read as a charge rather than a fraction: `5 / 7` made a reader do the division themselves to
   * learn what turning up was worth, where a level read at a glance and the multiplier beside it —
   * still the figure that matters, since it is what turnout actually buys — only confirms it.
   *
   * @param colony - The colony.
   * @returns The display-ready battery.
   */
  private batteryView(colony: Colony): ColonyBatteryView {
    const presence = colony.presence;
    const language = this.translation.language();
    const cells = Array.from(
      { length: presence.rosterSize },
      (_, index) => index < presence.present,
    );

    return {
      cellCount: presence.rosterSize,
      cells,
      isFull: presence.rosterSize > 0 && presence.present >= presence.rosterSize,
      multiplierLabel: formatMultiplier(presence.multiplier, language),
      ariaLabel: this.translation.translate('colony.track.presence.aria', {
        present: presence.present,
        roster: presence.rosterSize,
        threshold: presence.threshold,
      }),
      description: this.translation.translate('colony.track.presence.description', {
        threshold: presence.threshold,
      }),
      purpose: this.translation.translate('colony.track.presence.purpose', {
        roster: presence.rosterSize,
      }),
    };
  }

  /**
   * The attractivity bar: what morale is worth, read as tonight's arrivals rather than as a level.
   *
   * The bar still fills on morale itself — the fill is what a reader watches move fight over fight —
   * but the figure beside it used to be the same `55 / 100` the bar already draws, or the raw
   * percentage of the gap it closes, neither of which says what a reader actually wants to know:
   * how many are moving in tonight. `colony.populationChange` already carries that, read a second
   * time as this bar's own cause rather than only as the hexagon's consequence.
   *
   * @param colony - The colony.
   * @returns The display-ready bar.
   */
  private attractivityView(colony: Colony): ColonyAttractivityView {
    const morale = colony.morale;
    const language = this.translation.language();

    return {
      percentage: this.percentageOf(morale.value, morale.ceiling),
      moraleLabel: `${Math.round(morale.value)} / ${Math.round(morale.ceiling)}`,
      moraleValueLabel: `${Math.round(morale.value)}`,
      ariaLabel: this.translation.translate('colony.track.morale.aria', {
        value: Math.round(morale.value),
        ceiling: Math.round(morale.ceiling),
        arrivals: formatSignedPopulation(colony.populationChange, language),
      }),
      description: this.translation.translate('colony.track.morale.description'),
      purpose: this.translation.translate('colony.track.morale.purpose'),
    };
  }

  /**
   * Resolves one roster player into their turnout pip.
   *
   * @param player - The player and what they brought to today.
   * @returns The display-ready pip.
   */
  private toPip(player: ColonyPresencePlayer): ColonyPresencePipView {
    const fillClass =
      player.state === 'FULL'
        ? PRESENCE_PIP_FULL_CLASS
        : player.state === 'PARTIAL'
          ? PRESENCE_PIP_PARTIAL_CLASS
          : 'bg-surface-700';

    return {
      playerId: player.playerId,
      name: player.name,
      state: player.state,
      initials: player.name.slice(0, INITIALS_LENGTH).toUpperCase(),
      fillClass,
      ariaLabel: this.translation.translate(`colony.presence.${player.state}`, {
        name: player.name,
      }),
    };
  }

  /**
   * Resolves one step of the ladder into its row.
   *
   * @param tier - The step.
   * @param colony - The colony climbing it.
   * @param isNext - Whether this is the step the town is climbing towards.
   * @returns The display-ready step.
   */
  private toTierStep(tier: ColonyTier, colony: Colony, isNext: boolean): ColonyTierStepView {
    const language = this.translation.language();
    const missing = Math.max(0, tier.materialsRequired - colony.materials);

    return {
      threshold: tier.threshold,
      state: tier.state,
      isNext,
      name: this.tierName(tier),
      glyph: tierGlyphFor(tier),
      // What the step still costs, in the currency challenges and bosses pay. The column used to
      // carry the step's efficiency — a bare `8,75` naming no unit, appearing nowhere else on the
      // page, and that nothing the squad does moves directly. A step already paid prints a `0`
      // rather than nothing: the column is headed "left to gather", and a blank there left the town's
      // own step looking unsettled next to the figures below it.
      missingMaterialsLabel: formatPopulation(missing, language),
      // The bar belongs to the step being climbed, not to the one the town stands in: it is that
      // step's own gauge, read against the cost written beside it on the same row.
      progressPercentage: isNext ? colony.tierProgressPercentage : null,
      progressLabel: isNext
        ? this.translation.translate('colony.tierProgress', {
            name: this.tierName(tier),
            materials: formatPopulation(missing, language),
          })
        : '',
    };
  }

  /**
   * Resolves one week of the run into what its territory tile shows.
   *
   * @param week - The week and what its fight was worth.
   * @returns The display-ready fight.
   */
  private toBossView(week: ColonyWeek): ColonyBossView {
    const language = this.translation.language();

    return {
      weekIndex: week.weekIndex,
      state: week.state,
      // Two states show nothing. A week whose boss held settled nothing, and a `0` there competes
      // with the figures of the weeks that did pay. A week still locked has no boss drawn yet, so
      // the backend prices it at zero as well — writing that on the tile quoted every week ahead at
      // nothing, which reads as a promise of nothing rather than as an unknown. What is banked
      // belongs on a settled tile, what is on the table on the tile being fought, and a locked tile
      // carries neither.
      materialsLabel:
        week.state === 'SURVIVED' || week.state === 'UPCOMING'
          ? ''
          : formatPopulation(week.materials, language),
      efficiencyLabel:
        week.state === 'SURVIVED' || week.state === 'UPCOMING'
          ? ''
          : formatEfficiencyGain(week.efficiencyGain, language),
      earned: week.state === 'DEFEATED',
      moraleLabel: formatSignedPopulation(week.moraleDelta, language),
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

    return {
      runNumber: colony.runNumber,
      label: this.translation.translate('colony.history.run', { run: colony.runNumber }),
      isCurrent: true,
      finalLabel: formatPopulation(colony.population, this.translation.language()),
    };
  }

  /**
   * Resolves one closed run into a history row.
   *
   * @param run - The closed run.
   * @returns The display-ready run.
   */
  private toClosedRunView(run: ColonyRunHistory): ColonyRunView {
    return {
      runNumber: run.runNumber,
      label: this.translation.translate('colony.history.run', { run: run.runNumber }),
      isCurrent: false,
      finalLabel: formatPopulation(run.finalPopulation, this.translation.language()),
    };
  }

  /**
   * Names one step of the ladder, numbering it once the names start repeating.
   *
   * @param tier - The step, or a milestone carrying the same two fields.
   * @returns The already-translated name.
   */
  private tierName(tier: { name: string; level: number }): string {
    return this.translation.translate(`colony.tier.${tier.name}`, { level: tier.level });
  }

  /**
   * Formats one figure of the colony, or returns nothing while it has not resolved.
   *
   * @param pick - Which figure to read.
   * @returns The grouped figure.
   */
  private grouped(pick: (colony: Colony) => number): string {
    const colony = this.colony();

    return colony === null ? '' : formatPopulation(pick(colony), this.translation.language());
  }

  /**
   * Reads one figure as a share of another, clamped to the track it is drawn on.
   *
   * Clamped rather than left free because both ends really do happen: an empty run divides by a
   * housing of zero, and a town whose food outruns its housing produces a share above one hundred
   * that would otherwise run a band past the end of its rail.
   *
   * @param value - Figure to read.
   * @param total - Figure it is read against.
   * @returns The share, in `[0, 100]`.
   */
  private percentageOf(value: number, total: number): number {
    return total <= 0 ? 0 : Math.min(100, Math.max(0, (value / total) * 100));
  }
}
