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

import {
  AXIS_TICK_FONT,
  axisTitleOptions,
  chartPixelRatio,
  chartTooltipOptions,
  ChartTheme,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
} from './chart-theme';
import { ChartSeries } from './chart.model';

/**
 * Plots one or more series against a shared, index-based axis.
 *
 * Built directly on Chart.js rather than on an Angular wrapper: the wrappers all pull
 * `@angular/cdk` in with them, which this application deliberately keeps out of its bundle.
 *
 * No legend is drawn on the canvas. Callers render one in HTML instead, so it can carry each
 * series' average beside its name and be read out with the rest of the page. Curves are not
 * end-labeled either, and that is a consequence of the layout rather than an omission: series of
 * different lengths are padded at the *front* so they all finish on the same abscissa, which would
 * stack every end label on the same pixel.
 */
@Component({
  selector: 'app-line-chart',
  templateUrl: './line-chart.html',
  host: { class: 'block' },
})
export class LineChart {
  /**
   * Curves to plot, in the order their colors were assigned.
   */
  public readonly series = input.required<readonly ChartSeries[]>();

  /**
   * Accessible name of the chart.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Already-translated prose standing in for the plot itself, read out to assistive technology.
   */
  public readonly summary = input('');

  /**
   * Unit appended to every value in the tooltip, such as a percent sign. Empty for bare numbers.
   */
  public readonly valueSuffix = input('');

  /**
   * Already-translated name of the x axis unit, used as the tooltip's title.
   */
  public readonly pointLabel = input('');

  /**
   * Already-translated name of the x axis. Empty leaves the axis unnamed.
   */
  public readonly xAxisLabel = input('');

  /**
   * Already-translated name of the y axis. Empty leaves the axis unnamed.
   */
  public readonly yAxisLabel = input('');

  /**
   * Canvas the chart paints on.
   */
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * The live chart, or `null` until the first render has produced a canvas to attach to.
   */
  private chart: Chart<'line'> | null = null;

  /**
   * Length of the shared axis: the longest series, since the shorter ones are padded up to it.
   */
  private readonly pointCount = computed(() =>
    this.series().reduce((longest, series) => Math.max(longest, series.points.length), 0),
  );

  /**
   * Creates the chart once the canvas exists and keeps it in step with the inputs afterwards.
   */
  constructor() {
    registerChartComponents();

    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.configuration(resolveChartTheme()));
      // The canvas sits in a responsive grid that has not finished settling when the chart is
      // built, so its backing store is sized for a box narrower than the one it ends up in and the
      // browser stretches the result. One resize on the next frame, once the layout is final,
      // repaints it at its real size.
      requestAnimationFrame(() => this.chart?.resize());
    });

    // Repaints on every input change. `update('none')` skips the animation: the first draw earns
    // one, but replaying it every time the reader swaps the plotted metric turns a comparison into
    // a wait.
    effect(() => {
      const datasets = this.datasets();
      const labels = this.labels();
      const yAxisLabel = this.yAxisLabel();
      if (!this.chart) {
        return;
      }
      this.chart.data.labels = labels;
      this.chart.data.datasets = datasets;
      // Follows the swapped metric: the y axis is named after whatever is currently plotted.
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
   * Builds the x-axis labels, one per position on the shared axis.
   *
   * @returns the axis labels
   */
  private labels(): string[] {
    return Array.from({ length: this.pointCount() }, (_, index) => String(index + 1));
  }

  /**
   * Maps the input series onto Chart.js datasets.
   *
   * @returns the datasets to plot
   */
  private datasets(): ChartConfiguration<'line'>['data']['datasets'] {
    return this.series().map((series) => ({
      label: series.label,
      data: [...series.points],
      borderColor: series.color,
      backgroundColor: series.color,
      borderWidth: 2,
      // A season runs to hundreds of matches: a marker on every one of them would fuse into a
      // solid band and bury the trend the chart exists to show.
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBorderWidth: 0,
      tension: 0.2,
      spanGaps: false,
    }));
  }

  /**
   * Assembles the chart configuration.
   *
   * @param theme resolved chart palette
   * @returns the configuration handed to Chart.js
   */
  private configuration(theme: ChartTheme): ChartConfiguration<'line'> {
    return {
      type: 'line',
      data: { labels: this.labels(), datasets: this.datasets() },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        // Indexed rather than nearest-point: with several seasons plotted the reader is comparing
        // them at one abscissa, so all of them belong in the same bubble.
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
              label: (item) => `${item.dataset.label}: ${item.formattedValue}${this.valueSuffix()}`,
            },
          },
        },
      },
      plugins: [LineChart.crosshair(theme)],
    };
  }

  /**
   * Builds the vertical rule following the pointer.
   *
   * Chart.js has no crosshair of its own, and an indexed tooltip without one leaves the reader
   * guessing which abscissa the figures belong to on a chart hundreds of points wide.
   *
   * @param theme resolved chart palette
   * @returns the crosshair plugin, scoped to one chart instance
   */
  private static crosshair(theme: ChartTheme): Plugin<'line'> {
    return {
      id: 'crosshair',
      afterDatasetsDraw(chart) {
        const active = chart.tooltip?.getActiveElements() ?? [];
        if (active.length === 0) {
          return;
        }

        const { ctx, chartArea } = chart;
        ctx.save();
        ctx.beginPath();
        ctx.lineWidth = 1;
        ctx.strokeStyle = theme.grid;
        ctx.moveTo(active[0].element.x, chartArea.top);
        ctx.lineTo(active[0].element.x, chartArea.bottom);
        ctx.stroke();
        ctx.restore();
      },
    };
  }
}
