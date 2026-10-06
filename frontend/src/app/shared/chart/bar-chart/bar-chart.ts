import {
  afterNextRender,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  viewChild,
} from '@angular/core';

import { Chart, ChartConfiguration, Plugin } from 'chart.js';

import { resolveLocale } from '@core/i18n/format/locale.utils';
import { formatNumber } from '@core/i18n/format/number-format.utils';
import { Translation } from '@core/i18n/translation';

import { ChartBar, ChartValueFormatter } from '../chart.model';
import { AXIS_TICK_FONT } from '../chart-theme.constants';
import {
  axisTitleOptions,
  chartPixelRatio,
  chartTooltipOptions,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
} from '../chart-theme.utils';

/**
 * One categorical series as bars; no legend, the highlighted bar prints its value.
 */
@Component({
  selector: 'app-bar-chart',
  templateUrl: './bar-chart.html',
  styleUrl: './bar-chart.scss',
  host: { class: 'block' },
})
export class BarChart {
  /**
   * Bars to plot, in display order.
   */
  public readonly bars = input.required<readonly ChartBar[]>();

  /**
   * Accessible name of the chart.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Translated prose read to assistive technology in place of the plot.
   */
  public readonly summary = input('');

  /**
   * Tooltip value formatter, unit included; defaults to the localized number, one decimal.
   */
  public readonly valueFormatter = input<ChartValueFormatter | null>(null);

  /**
   * Translated x axis name, empty for none.
   */
  public readonly xAxisLabel = input('');

  /**
   * Translated y axis name, empty for none.
   */
  public readonly yAxisLabel = input('');

  /**
   * Prints every bar's value, not only the highlighted one's.
   */
  public readonly showAllValues = input(false);

  /**
   * Translation service, for the chart locale and default number format.
   */
  private readonly translation = inject(Translation);

  /**
   * Canvas the chart paints on.
   */
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * Live chart, `null` until the first render.
   */
  private chart: Chart<'bar'> | null = null;

  /**
   * Palette resolved once, shared by the configuration and the label plugin.
   */
  private readonly theme = resolveChartTheme();

  /**
   * Creates the chart after render and syncs it with the inputs.
   */
  constructor() {
    registerChartComponents();

    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.configuration());
      // The grid is still settling at build time: resize next frame to avoid a stretched canvas.
      requestAnimationFrame(() => this.chart?.resize());
    });

    effect(() => {
      const bars = this.bars();
      if (!this.chart) {
        return;
      }
      this.chart.data.labels = bars.map((bar) => bar.label);
      this.chart.data.datasets = this.datasets();
      this.chart.update('none');
    });

    // Cleared so the pending resize frame finds no chart, as a destroyed one has no canvas.
    inject(DestroyRef).onDestroy(() => {
      this.chart?.destroy();
      this.chart = null;
    });
  }

  /**
   * Input bars as a single Chart.js dataset.
   */
  private datasets(): ChartConfiguration<'bar'>['data']['datasets'] {
    const bars = this.bars();
    return [
      {
        label: this.ariaLabel(),
        data: bars.map((bar) => bar.value),
        backgroundColor: bars.map((bar) => this.fill(bar)),
        borderWidth: 0,
        // Rounded on the data end only, so the bar stays anchored to its baseline.
        borderRadius: { topLeft: 4, topRight: 4, bottomLeft: 0, bottomRight: 0 },
        maxBarThickness: 44,
        categoryPercentage: 0.7,
        barPercentage: 0.9,
      },
    ];
  }

  /**
   * Fill color of a bar.
   */
  private fill(bar: ChartBar): string {
    if (bar.muted) {
      return this.theme.muted;
    }
    return bar.highlighted ? this.theme.highlight : this.theme.bar;
  }

  /**
   * Chart.js configuration.
   */
  private configuration(): ChartConfiguration<'bar'> {
    const theme = this.theme;
    return {
      type: 'bar',
      data: { labels: this.bars().map((bar) => bar.label), datasets: this.datasets() },
      options: {
        locale: resolveLocale(this.translation.language()),
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        interaction: { mode: 'index', intersect: false },
        // Room above the tallest bar for its value label.
        layout: { padding: { top: 20 } },
        scales: {
          x: {
            grid: { display: false },
            border: { color: theme.grid },
            title: axisTitleOptions(theme, this.xAxisLabel()),
            ticks: {
              color: theme.tick,
              maxRotation: 0,
              autoSkip: false,
              font: AXIS_TICK_FONT,
            },
          },
          y: {
            beginAtZero: true,
            grid: { color: theme.grid },
            border: { display: false },
            title: axisTitleOptions(theme, this.yAxisLabel()),
            ticks: {
              color: theme.tick,
              maxTicksLimit: 5,
              font: AXIS_TICK_FONT,
            },
          },
        },
        plugins: {
          tooltip: {
            ...chartTooltipOptions(theme),
            callbacks: {
              label: (item) => this.formatValue(item.parsed.y ?? 0),
              afterLabel: (item) => this.bars()[item.dataIndex]?.detail ?? '',
            },
          },
        },
      },
      plugins: [this.highlightLabel()],
    };
  }

  /**
   * Prints the highlighted bar's value (all with `showAllValues`), never a muted bar's.
   */
  private highlightLabel(): Plugin<'bar'> {
    const bars = (): readonly ChartBar[] => this.bars();
    const format = (value: number): string => this.formatValue(value);
    const showAll = (): boolean => this.showAllValues();
    const theme = this.theme;

    return {
      id: 'highlightLabel',
      afterDatasetsDraw(chart) {
        const meta = chart.getDatasetMeta(0).data;
        const { ctx } = chart;
        ctx.save();
        ctx.font = '600 15px "Barlow Condensed", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'bottom';

        bars().forEach((bar, index) => {
          if (bar.muted || (!showAll() && !bar.highlighted)) {
            return;
          }

          const element = meta[index];
          if (!element) {
            return;
          }

          ctx.fillStyle = bar.highlighted ? theme.highlight : theme.tick;
          ctx.fillText(bar.valueLabel ?? format(bar.value), element.x, element.y - 6);
        });

        ctx.restore();
      },
    };
  }

  /**
   * A plotted value as the tooltip writes it.
   */
  private formatValue(value: number): string {
    const formatter = this.valueFormatter();
    return formatter
      ? formatter(value)
      : formatNumber(value, resolveLocale(this.translation.language()), {
          maximumFractionDigits: 1,
        });
  }
}
