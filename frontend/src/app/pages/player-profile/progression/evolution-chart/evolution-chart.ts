import { Component, computed, inject, input, signal } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { formatSeasonName } from '@core/matches/season-name.utils';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { SeasonEvolution } from '@core/players/player-progression.model';
import { LineChart } from '@shared/chart/line-chart';
import { resolveSeriesColor } from '@shared/chart/chart-theme';
import { Tooltip } from '@shared/tooltip/tooltip';
import {
  buildEvolutionSeries,
  EVOLUTION_METRICS,
  EvolutionMetric,
} from '../evolution-series.utils';

/**
 * One row of the chart's legend: a season, its color, and the average it held over that season.
 */
interface EvolutionLegendEntry {
  readonly label: string;
  readonly color: string;
  readonly average: string;
}

/**
 * Match-by-match evolution of one metric, across the selected seasons.
 *
 * One chart area, four metrics, swapped by the buttons above it — rather than four charts stacked
 * down the page, which would leave the reader scrolling to compare a season against itself.
 */
@Component({
  selector: 'app-evolution-chart',
  imports: [TranslatePipe, LineChart, Tooltip],
  templateUrl: './evolution-chart.html',
})
export class EvolutionChart {
  /**
   * Per-season series, as the API returned them.
   */
  public readonly evolution = input.required<readonly SeasonEvolution[]>();

  /**
   * Every known season's identifier, newest first.
   *
   * The source of a season's color: taking it from the position in this list rather than in the
   * current selection is what stops a curve changing color when the reader unticks another season.
   */
  public readonly seasonOrder = input.required<readonly number[]>();

  /**
   * i18n service, used for the labels the chart hands to the canvas.
   */
  private readonly translation = inject(Translation);

  /**
   * Metric currently plotted.
   */
  protected readonly metric = signal<EvolutionMetric>('headshotPercentage');

  /**
   * Metrics offered by the swap buttons.
   */
  protected readonly metrics = EVOLUTION_METRICS;

  /**
   * Curves handed to the chart, padded so every season ends on the same abscissa.
   */
  protected readonly series = computed(() =>
    buildEvolutionSeries(this.evolution(), this.metric(), (seasonId) =>
      resolveSeriesColor(Math.max(0, this.seasonOrder().indexOf(seasonId))),
    ).map((series) => ({ ...series, label: this.seasonLabel(series.label) })),
  );

  /**
   * Legend rows, one per plotted season.
   *
   * Rendered in HTML rather than on the canvas so each row can carry the season's average, which
   * is the figure that makes two curves comparable at all.
   */
  protected readonly legend = computed<readonly EvolutionLegendEntry[]>(() =>
    this.evolution().map((season, index) => ({
      label: this.seasonLabel(season.seasonName),
      color: this.series()[index]?.color ?? resolveSeriesColor(0),
      average: this.format(season.averages[this.metric()]),
    })),
  );

  /**
   * Unit appended to the plotted values in the tooltip.
   */
  protected readonly valueSuffix = computed(() =>
    this.metric() === 'headshotPercentage' ? '%' : '',
  );

  /**
   * Name of the y axis: whichever metric is currently plotted, since the axis changes meaning with
   * every swap and an unnamed one would leave the reader guessing what 230 stands for.
   */
  protected readonly yAxisLabel = computed(() =>
    this.translation.translate(`playerProfile.progression.evolution.axis.${this.metric()}`),
  );

  /**
   * Prose standing in for the plot, read out to assistive technology.
   */
  protected readonly summary = computed(() =>
    this.legend()
      .map((entry) =>
        this.translation.translate('playerProfile.progression.evolution.legendSummary', {
          season: entry.label,
          average: entry.average,
        }),
      )
      .join(' '),
  );

  /**
   * Whether more than one season is plotted, and the front-padding is therefore in play.
   */
  protected readonly isComparing = computed(() => this.evolution().length > 1);

  /**
   * Switches the plotted metric.
   *
   * @param metric - The newly selected metric.
   */
  protected onMetricChange(metric: EvolutionMetric): void {
    this.metric.set(metric);
  }

  /**
   * Formats a value of the plotted metric, in the same shape the profile's tiles use.
   *
   * @param value - The value to format.
   * @returns The formatted value.
   */
  protected format(value: number): string {
    switch (this.metric()) {
      case 'headshotPercentage':
        return formatHeadshotPercentage(value);
      case 'kda':
        return formatKda(value);
      default:
        return formatScore(value);
    }
  }

  /**
   * Spells a season's raw code out in the active language.
   *
   * @param name - The raw season name, as returned by the API.
   * @returns The label shown in the legend, on the curves and in the tooltip.
   */
  private seasonLabel(name: string): string {
    return formatSeasonName(name, (key, params) => this.translation.translate(key, params));
  }
}
