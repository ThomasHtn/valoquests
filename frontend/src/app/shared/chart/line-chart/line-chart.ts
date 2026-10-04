import {
  afterNextRender,
  Component,
  computed,
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

import { AXIS_TICK_FONT } from '../chart-theme.constants';
import {
  axisTitleOptions,
  chartPixelRatio,
  chartTooltipOptions,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
  resolveCssColor,
} from '../chart-theme.utils';
import { ChartTheme } from '../chart-theme.model';
import { createCrosshairPlugin } from '../chart-plugins.utils';
import { ChartSeries, ChartValueFormatter } from '../chart.model';

/**
 * Series on a shared index axis, on raw Chart.js (wrappers pull in `@angular/cdk`).
 * No legend (callers render one in HTML) and no end labels (front-padded series end on one pixel).
 */
@Component({
  selector: 'app-line-chart',
  templateUrl: './line-chart.html',
  host: { class: 'block' },
})
export class LineChart {
  /**
   * Curves to plot, in color assignment order.
   */
  public readonly series = input.required<readonly ChartSeries[]>();

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
   * Translation service, for the chart locale and default number format.
   */
  private readonly translation = inject(Translation);

  /**
   * Translated x axis unit, used as the tooltip title.
   */
  public readonly pointLabel = input('');

  /**
   * Translated x axis name, empty for none.
   */
  public readonly xAxisLabel = input('');

  /**
   * Translated y axis name, empty for none.
   */
  public readonly yAxisLabel = input('');

  /**
   * Tailwind height classes of the chart box; tile previews pass a shorter one.
   */
  public readonly heightClass = input('h-64 w-full sm:h-72');

  /**
   * Formatted value marked by a dashed rule at the first series' peak, empty for none.
   */
  public readonly peakLabel = input('');

  /**
   * Translated x labels, one per point; empty falls back to `1, 2, 3…`.
   */
  public readonly xLabels = input<readonly string[]>([]);

  /**
   * Tints the area under each curve; off by default since overlaid fills bury the lines.
   */
  public readonly filled = input(false);

  /**
   * Canvas the chart paints on.
   */
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * Live chart, `null` until the first render.
   */
  private chart: Chart<'line'> | null = null;

  /**
   * Shared axis length: the longest series, shorter ones are padded to it.
   */
  private readonly pointCount = computed(() =>
    this.series().reduce((longest, series) => Math.max(longest, series.points.length), 0),
  );

  /**
   * Creates the chart after render and syncs it with the inputs.
   */
  constructor() {
    registerChartComponents();

    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.configuration(resolveChartTheme()));
      // The grid is still settling at build time: resize next frame to avoid a stretched canvas.
      requestAnimationFrame(() => this.chart?.resize());
    });

    // `update('none')`: only the first draw animates, swapping metrics must be instant.
    effect(() => {
      const datasets = this.datasets();
      const labels = this.labels();
      const yAxisLabel = this.yAxisLabel();
      if (!this.chart) {
        return;
      }
      this.chart.data.labels = labels;
      this.chart.data.datasets = datasets;
      const yTitle = this.chart.options.scales?.['y']?.title;
      if (yTitle) {
        yTitle.display = yAxisLabel.length > 0;
        yTitle.text = yAxisLabel;
      }
      this.chart.update('none');
    });

    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  /**
   * X axis labels, one per position.
   */
  private labels(): string[] {
    const custom = this.xLabels();
    if (custom.length > 0) {
      return [...custom];
    }

    return Array.from({ length: this.pointCount() }, (_, index) => String(index + 1));
  }

  /**
   * Input series as Chart.js datasets.
   */
  private datasets(): ChartConfiguration<'line'>['data']['datasets'] {
    return this.series().map((series) => {
      const filled = series.filled ?? this.filled();
      return {
        label: series.label,
        data: [...series.points],
        borderColor: series.color,
        // Resolved to a literal: canvas cannot compute `color-mix()`.
        backgroundColor: filled
          ? resolveCssColor(`color-mix(in oklab, ${series.color} 20%, transparent)`)
          : series.color,
        borderWidth: 2,
        borderDash: series.dashed ? [4, 4] : [],
        // Hundreds of points: markers would fuse into a solid band.
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBorderWidth: 0,
        tension: 0.2,
        spanGaps: false,
        // 'origin' is clipped to the chart's bottom edge when zero is off scale.
        fill: filled ? 'origin' : false,
      };
    });
  }

  /**
   * Chart.js configuration for the given palette.
   */
  private configuration(theme: ChartTheme): ChartConfiguration<'line'> {
    return {
      type: 'line',
      data: { labels: this.labels(), datasets: this.datasets() },
      options: {
        locale: resolveLocale(this.translation.language()),
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        // Index mode: series are compared at one abscissa, so they share a tooltip.
        interaction: { mode: 'index', intersect: false },
        scales: {
          x: {
            grid: { display: false },
            border: { color: theme.grid },
            title: axisTitleOptions(theme, this.xAxisLabel()),
            ticks: {
              color: theme.tick,
              maxTicksLimit: 8,
              autoSkip: true,
              maxRotation: 0,
              font: AXIS_TICK_FONT,
            },
          },
          y: {
            grid: { color: theme.grid },
            border: { display: false },
            title: axisTitleOptions(theme, this.yAxisLabel()),
            ticks: {
              color: theme.tick,
              maxTicksLimit: 6,
              font: AXIS_TICK_FONT,
            },
          },
        },
        plugins: {
          tooltip: {
            ...chartTooltipOptions(theme),
            callbacks: {
              title: (items) => `${this.pointLabel()} ${items[0]?.label ?? ''}`.trim(),
              label: (item) => `${item.dataset.label}: ${this.formatValue(item.parsed.y ?? 0)}`,
            },
          },
        },
      },
      plugins: [createCrosshairPlugin(theme), this.peakMarker(theme)],
    };
  }

  /**
   * Dashed rule at the first series' peak; reads {@link peakLabel} at draw time to stay live.
   */
  private peakMarker(theme: ChartTheme): Plugin<'line'> {
    return {
      id: 'peakMarker',
      afterDatasetsDraw: (chart) => {
        const label = this.peakLabel();
        const dataset = chart.data.datasets[0];
        if (label.length === 0 || !dataset) {
          return;
        }

        const points = dataset.data as (number | null)[];
        let peakIndex = -1;
        let peakValue = -Infinity;
        points.forEach((value, index) => {
          if (value !== null && value > peakValue) {
            peakValue = value;
            peakIndex = index;
          }
        });
        const point = peakIndex === -1 ? undefined : chart.getDatasetMeta(0).data[peakIndex];
        if (!point) {
          return;
        }

        const { ctx, chartArea } = chart;
        const color = typeof dataset.borderColor === 'string' ? dataset.borderColor : theme.tick;

        ctx.save();
        ctx.setLineDash([4, 4]);
        ctx.lineWidth = 1;
        ctx.strokeStyle = theme.grid;
        ctx.beginPath();
        ctx.moveTo(chartArea.left, point.y);
        ctx.lineTo(chartArea.right, point.y);
        ctx.stroke();

        ctx.setLineDash([]);
        ctx.font = `${AXIS_TICK_FONT.size}px ${AXIS_TICK_FONT.family}`;
        ctx.fillStyle = color;
        ctx.textAlign = 'left';
        ctx.textBaseline = 'bottom';
        ctx.fillText(label, chartArea.left + 4, Math.max(chartArea.top + 12, point.y - 4));
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
