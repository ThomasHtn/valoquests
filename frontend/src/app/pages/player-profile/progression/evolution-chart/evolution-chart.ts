import { Component, computed, inject, input, signal } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { SeasonEvolution } from '@core/players/progression/player-progression.model';
import { formatSeasonName } from '@core/seasons/season-name.utils';
import { resolveSeriesColor } from '@shared/chart/chart-theme.utils';
import { LineChart } from '@shared/chart/line-chart/line-chart';
import { Tooltip } from '@shared/tooltip/tooltip';

import { EVOLUTION_METRICS } from './evolution-chart.constants';
import { EvolutionLegendEntry, EvolutionMetric } from './evolution-chart.model';
import { buildEvolutionSeries } from './evolution-chart-series.utils';

/**
 * Match-by-match evolution of one metric across the selected seasons, swapped by buttons.
 */
@Component({
  selector: 'app-evolution-chart',
  imports: [TranslatePipe, LineChart, Tooltip],
  templateUrl: './evolution-chart.html',
  styleUrl: './evolution-chart.scss',
})
export class EvolutionChart {
  /**
   * Per-season series, as the API returned them.
   */
  public readonly evolution = input.required<readonly SeasonEvolution[]>();

  /**
   * Every season id, newest first; colours come from this order so a curve keeps its colour.
   */
  public readonly seasonOrder = input.required<readonly number[]>();

  /**
   * Translates the labels handed to the canvas.
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
   * Legend rows in HTML, so each carries the season average that makes curves comparable.
   */
  protected readonly legend = computed<readonly EvolutionLegendEntry[]>(() =>
    this.evolution().map((season, index) => ({
      label: this.seasonLabel(season.seasonName),
      color: this.series()[index]?.color ?? resolveSeriesColor(0),
      average: this.format(season.averages[this.metric()]),
    })),
  );

  /**
   * Y axis name: the plotted metric, since the axis changes meaning with every swap.
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
   * Whether several seasons are plotted, so the front padding is in play.
   */
  protected readonly isComparing = computed(() => this.evolution().length > 1);

  /**
   * Formats a tooltip value like the profile's tiles, unit included.
   */
  protected readonly valueFormatter = (value: number): string => this.format(value);

  /**
   * Switches the plotted metric.
   */
  protected onMetricChange(metric: EvolutionMetric): void {
    this.metric.set(metric);
  }

  /**
   * Formats a value of the plotted metric like the profile's tiles.
   */
  private format(value: number): string {
    switch (this.metric()) {
      case 'headshotPercentage':
        return formatHeadshotPercentage(value, this.translation.language());
      case 'kda':
        return formatKda(value, this.translation.language());
      default:
        return formatScore(value);
    }
  }

  /**
   * Season name in the active language, for the legend, curves and tooltip.
   */
  private seasonLabel(name: string): string {
    return formatSeasonName(name, (key, params) => this.translation.translate(key, params));
  }
}
