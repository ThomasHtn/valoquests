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

import {
  AXIS_TICK_FONT,
  axisTitleOptions,
  chartPixelRatio,
  chartTooltipOptions,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
} from './chart-theme';
import { ChartBar } from './chart.model';

/**
 * Plots one categorical series as bars.
 *
 * One series only, so no legend: the block's own heading names what is being measured, and a
 * legend box repeating it would be furniture. Identity never rests on color alone — the
 * highlighted bar carries its value in plain text beside it, and the axis names every category.
 */
@Component({
  selector: 'app-bar-chart',
  templateUrl: './bar-chart.html',
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
   * Already-translated prose standing in for the plot itself, read out to assistive technology.
   */
  public readonly summary = input('');

  /**
   * Unit appended to every value, such as a percent sign. Empty for bare numbers.
   */
  public readonly valueSuffix = input('');

  /**
   * Already-translated name of the x axis. Empty leaves the axis unnamed.
   */
  public readonly xAxisLabel = input('');

  /**
   * Already-translated name of the y axis. Empty leaves the axis unnamed.
   */
  public readonly yAxisLabel = input('');

  /**
   * Tailwind classes sizing the chart's box, so a caller with a tighter seat than the chart's own
   * default can still fit it.
   */
  public readonly heightClass = input('h-52 w-full sm:h-56');

  /**
   * Prints every bar's own value above it, rather than only the highlighted one's. For a chart with
   * no single "best" bar to single out — every bar is its own reading, not a contender against the
   * others.
   */
  public readonly showAllValues = input(false);

  /**
   * Canvas the chart paints on.
   */
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * The live chart, or `null` until the first render has produced a canvas to attach to.
   */
  private chart: Chart<'bar'> | null = null;

  /**
   * Palette resolved once, shared by the configuration and the value-label plugin.
   */
  private readonly theme = resolveChartTheme();

  /**
   * Creates the chart once the canvas exists and keeps it in step with the inputs afterwards.
   */
  constructor() {
    registerChartComponents();

    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.configuration());
      // The canvas sits in a responsive grid that has not finished settling when the chart is
      // built, so its backing store is sized for a box narrower than the one it ends up in and the
      // browser stretches the result. One resize on the next frame, once the layout is final,
      // repaints it at its real size.
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

    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  /**
   * Maps the input bars onto a single Chart.js dataset.
   *
   * @returns the dataset to plot
   */
  private datasets(): ChartConfiguration<'bar'>['data']['datasets'] {
    const bars = this.bars();
    return [
      {
        label: this.ariaLabel(),
        data: bars.map((bar) => bar.value),
        backgroundColor: bars.map((bar) => this.fill(bar)),
        borderWidth: 0,
        // A 4px cap on the data end only, so the bar stays anchored to its baseline instead of
        // floating as a lozenge.
        borderRadius: { topLeft: 4, topRight: 4, bottomLeft: 0, bottomRight: 0 },
        maxBarThickness: 44,
        categoryPercentage: 0.7,
        barPercentage: 0.9,
      },
    ];
  }

  /**
   * Picks a bar's fill.
   *
   * @param bar the bar being drawn
   * @returns the fill color
   */
  private fill(bar: ChartBar): string {
    if (bar.muted) {
      return this.theme.muted;
    }
    return bar.highlighted ? this.theme.highlight : 'rgb(217 149 74 / 0.55)';
  }

  /**
   * Assembles the chart configuration.
   *
   * @returns the configuration handed to Chart.js
   */
  private configuration(): ChartConfiguration<'bar'> {
    const theme = this.theme;
    return {
      type: 'bar',
      data: { labels: this.bars().map((bar) => bar.label), datasets: this.datasets() },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        interaction: { mode: 'index', intersect: false },
        // Leaves room above the tallest bar for the highlighted bar's own label.
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
              label: (item) => `${item.formattedValue}${this.valueSuffix()}`,
              afterLabel: (item) => this.bars()[item.dataIndex]?.detail ?? '',
            },
          },
        },
      },
      plugins: [this.highlightLabel()],
    };
  }

  /**
   * Builds the plugin printing a bar's value above it.
   *
   * By default only the highlighted bar is labeled: a number over every bar is noise the axis
   * already carries, but the bar the section singles out has to say what makes it the best without
   * the reader hovering it — and without the claim resting on its color alone. `showAllValues`
   * flips that for a chart with no single "best" bar, where every bar's own reading is the point.
   * A muted bar (no sample yet) is never labeled either way — it has nothing to state.
   *
   * @returns the value-label plugin, scoped to one chart instance
   */
  private highlightLabel(): Plugin<'bar'> {
    const bars = (): readonly ChartBar[] => this.bars();
    const suffix = (): string => this.valueSuffix();
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
          ctx.fillText(`${bar.value}${suffix()}`, element.x, element.y - 6);
        });

        ctx.restore();
      },
    };
  }
}
