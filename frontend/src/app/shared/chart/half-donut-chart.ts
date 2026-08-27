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
import { Chart, ChartConfiguration } from 'chart.js';

import {
  chartPixelRatio,
  chartTooltipOptions,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
} from './chart-theme';
import { ChartGaugeSegment } from './chart.model';

/**
 * A gauge reading zero to a hundred as a half-circle, one arc per segment.
 *
 * The food ring and the attractivity dome: a stock read across seven days, and a single percentage
 * read against its own track. Both are the same shape — a `doughnut` chart held to its top
 * half-circle (`circumference: 180`) — so one component draws both, the day-by-day gaps and the
 * fill/track split alike being no more than how a caller slices `segments` into weights.
 *
 * Drawn on a canvas rather than as a hand-rolled `conic-gradient`/`clip-path` stack: the arcs, the
 * gaps between them and the hover target were built by hand once and the antialiasing on a curved
 * `clip-path` never matched a canvas's own. Chart.js already draws every other curve on this site,
 * so this is the same tool, not a new one.
 */
@Component({
  selector: 'app-half-donut-chart',
  templateUrl: './half-donut-chart.html',
  host: { class: 'relative block' },
})
export class HalfDonutChart {
  /**
   * The gauge's own arcs, in draw order clockwise from the left.
   */
  public readonly segments = input.required<readonly ChartGaugeSegment[]>();

  /**
   * Accessible name of the gauge.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Already-translated prose standing in for the plot itself, read out to assistive technology.
   */
  public readonly summary = input('');

  /**
   * Share of the gauge's own radius left hollow at its centre, as a CSS percentage such as `'65%'`.
   */
  public readonly cutout = input.required<string>();

  /**
   * Canvas the chart paints on.
   */
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * The live chart, or `null` until the first render has produced a canvas to attach to.
   */
  private chart: Chart<'doughnut'> | null = null;

  /**
   * Palette resolved once, shared by the configuration and the tooltip.
   */
  private readonly theme = resolveChartTheme();

  /**
   * Creates the chart once the canvas exists and keeps it in step with the inputs afterwards.
   */
  constructor() {
    registerChartComponents();

    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.configuration());
      // The canvas sits in a layout that has not finished settling when the chart is built, so its
      // backing store is sized for a box narrower than the one it ends up in and the browser
      // stretches the result. One resize on the next frame, once the layout is final, repaints it
      // at its real size.
      requestAnimationFrame(() => this.chart?.resize());
    });

    effect(() => {
      const segments = this.segments();
      if (!this.chart) {
        return;
      }
      this.chart.data.datasets = this.datasets();
      this.chart.data.labels = segments.map((segment) => segment.label);
      this.chart.update('none');
    });

    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  /**
   * Maps the input segments onto a single Chart.js dataset.
   *
   * @returns the dataset to plot
   */
  private datasets(): ChartConfiguration<'doughnut'>['data']['datasets'] {
    const segments = this.segments();

    return [
      {
        data: segments.map((segment) => segment.value),
        backgroundColor: segments.map((segment) => segment.color),
        borderWidth: 0,
        spacing: 3,
      },
    ];
  }

  /**
   * Assembles the chart configuration.
   *
   * @returns the configuration handed to Chart.js
   */
  private configuration(): ChartConfiguration<'doughnut'> {
    const theme = this.theme;
    const segments = this.segments();

    return {
      type: 'doughnut',
      data: { labels: segments.map((segment) => segment.label), datasets: this.datasets() },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        circumference: 180,
        rotation: -90,
        cutout: this.cutout(),
        layout: { padding: 0 },
        plugins: {
          tooltip: {
            ...chartTooltipOptions(theme),
            callbacks: {
              title: () => '',
              label: (item) => segments[item.dataIndex]?.label ?? '',
            },
          },
        },
      },
    };
  }
}
