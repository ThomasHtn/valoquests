import { computed, inject, Service, Signal } from '@angular/core';

import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Translation } from '@core/i18n/translation';
import { ChartBar } from '@shared/chart/chart.model';
import { ColonyApi } from './colony-api';
import { formatPopulation, formatSignedPopulation } from './colony-format.utils';
import { colonyTrackColors } from './colony-gauge.utils';
import {
  Colony,
  ColonyPresencePlayer,
  ColonyRunHistory,
  ColonyTier,
  ColonyWeek,
} from './colony.model';
import {
  ColonyBossView,
  ColonyDeltaView,
  ColonyPresencePipView,
  ColonyRunView,
  ColonyTierStepView,
  ColonyTrackGlyph,
  ColonyTrackView,
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
 * Morale from which the face in the socket smiles.
 *
 * Seventy: at least one fight's worth of morale ahead of the fifty a run opens on.
 */
const MORALE_GOOD = 70;

/**
 * Morale below which it frowns, close enough to the floor to say so.
 */
const MORALE_BAD = 40;

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
   * Materials banked, the run's one intermediate currency.
   */
  public readonly materialsLabel = computed<string>(() =>
    this.grouped((colony) => colony.materials),
  );

  /**
   * Share of the housing the population already fills, which is how high the hexagon is filled.
   *
   * Housing as the denominator, never the food ceiling: the hexagon is read against the ladder
   * beside it, and both count in housing. Clamped, since a town whose food outruns its housing
   * produces a share above one hundred that would otherwise run the fill past the silhouette.
   */
  public readonly populationPercentage = computed<number>(() => {
    const colony = this.colony();

    return colony === null ? 0 : this.percentageOf(colony.population, colony.capacity);
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
   * Says exactly what the shape says: the figure, the housing it is filling towards — a full
   * hexagon *is* the capacity reached, which is why there is no mark drawn on it — what the night
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
      capacity: formatPopulation(colony.capacity, language),
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
   * The three rails, food first.
   */
  public readonly tracks = computed<readonly ColonyTrackView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    return [this.foodTrack(colony), this.presenceTrack(colony), this.moraleTrack(colony)];
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
   * The steps of the ladder around the town's own, lowest first.
   */
  public readonly ladder = computed<readonly ColonyTierStepView[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    return colony.ladder.map((tier) => this.toTierStep(tier, colony));
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
        capacity: formatPopulation(point.capacity, language),
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
   * The food rail: what the town eats, what it has left to grow on, and the housing neither reaches.
   *
   * Three shapes in one track, and the reason the page needs no sentence. When the bright band
   * disappears the town stops growing; when the muted one runs past what comes in, it shrinks.
   *
   * @param colony - The colony.
   * @returns The display-ready rail.
   */
  private foodTrack(colony: Colony): ColonyTrackView {
    const language = this.translation.language();
    const colors = colonyTrackColors('FOOD');
    const feedable = formatPopulation(colony.feedablePopulation, language);
    const capacity = formatPopulation(colony.capacity, language);

    return {
      track: 'FOOD',
      label: this.translation.translate('colony.track.food.name'),
      percentage: this.percentageOf(colony.population, colony.capacity),
      secondaryPercentage: this.percentageOf(colony.feedablePopulation, colony.capacity),
      // Both numbers, in this order, because they are not of the same kind: the first is what the
      // meals allow, the second what the housing allows. One figure alone let a reader believe
      // 3 520 people lived in a town holding 2 400.
      valueLabel: `${feedable} / ${capacity}`,
      ariaLabel: this.translation.translate('colony.track.food.aria', {
        eaten: formatPopulation(colony.weeklyConsumption, language),
        surplus: formatPopulation(colony.weeklySurplus, language),
      }),
      // Two swatches, not three. The stock is not a band of the rail, it is the sum of the two, and
      // the band already says what it allows.
      legend: [
        {
          colorClass: colors.muted,
          label: this.translation.translate('colony.track.food.eaten', {
            food: formatPopulation(colony.weeklyConsumption, language),
          }),
        },
        {
          colorClass: colors.fill,
          label: this.translation.translate('colony.track.food.surplus', {
            food: formatPopulation(colony.weeklySurplus, language),
          }),
        },
      ],
      note: '',
      glyph: 'FOOD',
      socketClass: colors.fill,
      primaryClass: colors.muted,
      secondaryClass: colors.fill,
      textClass: colors.text,
    };
  }

  /**
   * The turnout rail: how many of the squad turned up, against the roster frozen on the run.
   *
   * @param colony - The colony.
   * @returns The display-ready rail.
   */
  private presenceTrack(colony: Colony): ColonyTrackView {
    const colors = colonyTrackColors('PRESENCE');
    const presence = colony.presence;

    return {
      track: 'PRESENCE',
      label: this.translation.translate('colony.track.presence.name'),
      percentage: this.percentageOf(presence.present, presence.rosterSize),
      secondaryPercentage: null,
      valueLabel: `${presence.present} / ${presence.rosterSize}`,
      ariaLabel: this.translation.translate('colony.track.presence.aria', {
        present: presence.present,
        roster: presence.rosterSize,
        threshold: presence.threshold,
      }),
      // The card shows the roster pip by pip instead of a swatch: who is missing is the whole point.
      legend: [],
      note: '',
      glyph: 'PRESENCE',
      socketClass: colors.fill,
      primaryClass: colors.fill,
      secondaryClass: '',
      textClass: colors.text,
    };
  }

  /**
   * The morale rail: the speed the town moves at, with its floor painted rather than implied.
   *
   * A bar reading `55 / 100` while filling to a different share of its track would make its own figure
   * a lie, so the unreachable slice under the floor is drawn in a muted version of the same colour, the
   * way the food rail splits what the town eats from what it grows on. With the floor down at 1 that
   * slice is a hairline, and the split reads as a plain fill.
   *
   * @param colony - The colony.
   * @returns The display-ready rail.
   */
  private moraleTrack(colony: Colony): ColonyTrackView {
    const colors = colonyTrackColors('MORALE');
    const morale = colony.morale;

    return {
      track: 'MORALE',
      label: this.translation.translate('colony.track.morale.name'),
      percentage: this.percentageOf(morale.floor, morale.ceiling),
      secondaryPercentage: this.percentageOf(morale.value, morale.ceiling),
      valueLabel: `${Math.round(morale.value)} / ${Math.round(morale.ceiling)}`,
      ariaLabel: this.translation.translate('colony.track.morale.aria', {
        value: Math.round(morale.value),
        ceiling: Math.round(morale.ceiling),
        floor: Math.round(morale.floor),
      }),
      legend: [
        {
          colorClass: colors.muted,
          label: this.translation.translate('colony.track.morale.floor', {
            floor: Math.round(morale.floor),
          }),
        },
      ],
      note: this.translation.translate('colony.track.morale.note'),
      glyph: this.moraleGlyph(morale.value),
      socketClass: colors.fill,
      primaryClass: colors.muted,
      secondaryClass: colors.fill,
      textClass: colors.text,
    };
  }

  /**
   * Picks the face the morale socket wears.
   *
   * @param morale - Today's morale.
   * @returns The glyph.
   */
  private moraleGlyph(morale: number): ColonyTrackGlyph {
    if (morale >= MORALE_GOOD) {
      return 'MORALE_GOOD';
    }

    return morale < MORALE_BAD ? 'MORALE_BAD' : 'MORALE_NEUTRAL';
  }

  /**
   * Resolves one roster player into their turnout pip.
   *
   * @param player - The player and what they brought to today.
   * @returns The display-ready pip.
   */
  private toPip(player: ColonyPresencePlayer): ColonyPresencePipView {
    const colors = colonyTrackColors('PRESENCE');
    const fillClass =
      player.state === 'FULL'
        ? colors.fill
        : player.state === 'PARTIAL'
          ? colors.muted
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
   * @returns The display-ready step.
   */
  private toTierStep(tier: ColonyTier, colony: Colony): ColonyTierStepView {
    const language = this.translation.language();
    const isCurrent = tier.state === 'CURRENT';

    return {
      threshold: tier.threshold,
      state: tier.state,
      name: this.tierName(tier),
      // The active step leads with what it is climbing towards; every other one carries the
      // threshold it opens at, which is all there is to say about a step nobody stands on. The
      // town's own housing led this row until it was read as the target: it is the one figure of
      // the row that moves, and the top of a row is where a target is looked for.
      valueLabel: formatPopulation(
        isCurrent ? colony.nextTier.threshold : tier.threshold,
        language,
      ),
      progressPercentage: isCurrent ? colony.tierProgressPercentage : null,
      progressLabel: isCurrent
        ? this.translation.translate('colony.tierProgress', {
            current: formatPopulation(colony.capacity, language),
            target: formatPopulation(colony.nextTier.threshold, language),
            missing: formatPopulation(colony.missingCapacity, language),
            name: this.tierName(colony.nextTier),
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
    const settled = week.state === 'DEFEATED' || week.state === 'SURVIVED';

    return {
      weekIndex: week.weekIndex,
      state: week.state,
      // A week not yet reached shows nothing at all: a `0` there would read as a fight already lost.
      housingLabel: settled
        ? formatSignedPopulation(week.state === 'DEFEATED' ? week.housingGain : 0, language)
        : '',
      housingEarned: week.state === 'DEFEATED',
      detailLabel: this.bossDetail(week, language),
    };
  }

  /**
   * Builds the sentence a territory tile's title carries.
   *
   * @param week - The week and what its fight was worth.
   * @param language - Active language.
   * @returns The already-translated sentence.
   */
  private bossDetail(week: ColonyWeek, language: 'fr' | 'en'): string {
    if (week.category === null) {
      return this.translation.translate('colony.boss.undrawn', { week: week.weekIndex });
    }

    return this.translation.translate(`colony.boss.detail.${week.state}`, {
      week: week.weekIndex,
      category: this.translation.translate(`colony.boss.category.${week.category}`),
      materials: formatPopulation(week.materials, language),
      housing: formatPopulation(week.housingGain, language),
      morale: formatSignedPopulation(week.moraleDelta, language),
    });
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
