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
  resolveCssColor,
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
   * Tailwind height utility of the chart's own box. Defaults to the full-page reading (the run's
   * curve, opened in a drawer); a caller drawing the same shape as a tile preview — the campaign
   * page's compact Trajectory tile — passes a shorter one instead.
   */
  public readonly heightClass = input('h-64 w-full sm:h-72');

  /**
   * Already-formatted value to mark at the first series' own highest point: a dashed rule through
   * it, the figure written beside it in the series' own color. Empty draws neither — the shape
   * every other chart in the app already keeps.
   */
  public readonly peakLabel = input('');

  /**
   * Already-translated label for each position on the shared axis, overriding the generated
   * `1, 2, 3…` index labels. Empty falls back to the index labels; a caller with a short,
   * meaningful axis — the food-this-week line's own weekdays — passes one entry per point instead.
   */
  public readonly xLabels = input<readonly string[]>([]);

  /**
   * Whether to shade the area between each curve and the chart's bottom edge, in a transparent
   * tint of the curve's own color. Off by default: with several curves overlaid a filled area
   * under every one of them would fuse into a wash and bury the lines underneath — only a caller
   * plotting a single curve (the food-this-week line) turns it on.
   */
  public readonly filled = input(false);

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
    const custom = this.xLabels();
    if (custom.length > 0) {
      return [...custom];
    }

    return Array.from({ length: this.pointCount() }, (_, index) => String(index + 1));
  }

  /**
   * Maps the input series onto Chart.js datasets.
   *
   * @returns the datasets to plot
   */
  private datasets(): ChartConfiguration<'line'>['data']['datasets'] {
    const filled = this.filled();

    return this.series().map((series) => ({
      label: series.label,
      data: [...series.points],
      borderColor: series.color,
      // Filled: a transparent tint of the line's own color, resolved to a literal value since
      // canvas cannot compute `color-mix()` itself. Unfilled: the flat color, unused as a fill.
      backgroundColor: filled
        ? resolveCssColor(`color-mix(in oklab, ${series.color} 20%, transparent)`)
        : series.color,
      borderWidth: 2,
      // A season runs to hundreds of matches: a marker on every one of them would fuse into a
      // solid band and bury the trend the chart exists to show.
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBorderWidth: 0,
      tension: 0.2,
      spanGaps: false,
      // 'origin' shades down to zero, clipped to the chart area's own bottom edge when zero falls
      // outside the visible scale — exactly the "between the point and the bottom" band asked for.
      fill: filled ? 'origin' : false,
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
      plugins: [LineChart.crosshair(theme), this.peakMarker(theme)],
    };
  }

  /**
   * Builds the dashed rule marking the first series' own highest point, with {@link peakLabel}
   * written beside it in the series' own color.
   *
   * Reads {@link peakLabel} at draw time rather than once at chart creation, so the mark stays
   * live across the `chart.update('none')` calls the input effect already drives — no separate
   * wiring needed for this one input.
   *
   * @param theme resolved chart palette
   * @returns the peak-marker plugin, scoped to one chart instance
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
