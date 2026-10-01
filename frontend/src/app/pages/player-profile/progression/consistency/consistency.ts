import {
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { LucideCalendar, LucideMap, LucideUser } from '@lucide/angular';
import { Chart, ScriptableContext } from 'chart.js';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TranslateFn } from '@core/i18n/translation.model';
import { ConsistencySummary } from '@core/players/player-progression.model';
import { ChartTooltip } from '@shared/chart/chart-tooltip';
import { trackChartTooltip } from '@shared/chart/chart-tooltip.utils';
import { AXIS_TICK_FONT } from '@shared/chart/chart-theme.constants';
import {
  axisTitleOptions,
  chartPixelRatio,
  prefersReducedMotion,
  registerChartComponents,
  resolveChartTheme,
  resolveSeriesColor,
} from '@shared/chart/chart-theme.utils';
import { ChartTooltipAnchor } from '@shared/chart/chart.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { KeyFigures } from '../key-figures/key-figures';
import {
  CONSISTENCY_AXIS_STEP,
  CONSISTENCY_BIN_WIDTH,
  CONSISTENCY_DOT_FILL,
  CONSISTENCY_I18N,
  CONSISTENCY_MIN_SAMPLE,
  CONSISTENCY_OUTSIDE_COLOR,
} from './consistency.constants';
import { ConsistencyAxis, ConsistencyDot } from './consistency.model';
import {
  buildConsistencyAxis,
  buildConsistencyFigures,
  buildConsistencyTooltip,
  createConsistencyBandPlugin,
  resolveConsistencyStackHeight,
  stackConsistencyDots,
} from './consistency.utils';

/**
 * How steady a player's combat score is over the seasons picked at the top of the page: one dot
 * per match, stacked in its score column, with the middle half of the matches between a floor and
 * a ceiling.
 */
@Component({
  selector: 'app-consistency',
  imports: [
    TranslatePipe,
    Tooltip,
    ChartTooltip,
    KeyFigures,
    LucideCalendar,
    LucideMap,
    LucideUser,
  ],
  templateUrl: './consistency.html',
})
export class Consistency {
  /**
   * Spread over the selection, or `null` when it holds too few matches.
   */
  public readonly consistency = input.required<ConsistencySummary | null>();

  /**
   * i18n service, used for the labels drawn on the canvas.
   */
  private readonly translation = inject(Translation);

  /**
   * Canvas the chart paints on, absent while there is no season to plot.
   */
  private readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('canvas');

  /**
   * The live chart, or `null` before the first draw.
   */
  private chart: Chart<'scatter', ConsistencyDot[]> | null = null;

  /**
   * Prefix of the translation keys the template reads.
   */
  protected readonly keys = CONSISTENCY_I18N;

  /**
   * Matches a season needs to be shown, quoted in the empty state.
   */
  protected readonly minimumSample = CONSISTENCY_MIN_SAMPLE;

  /**
   * Axis of the chart, or `null` without a spread.
   */
  private readonly axis = computed(() => {
    const consistency = this.consistency();
    return consistency ? buildConsistencyAxis(consistency) : null;
  });

  /**
   * One dot per match.
   */
  private readonly dots = computed(() => {
    const consistency = this.consistency();
    const axis = this.axis();
    return consistency && axis ? stackConsistencyDots(consistency, axis) : [];
  });

  /**
   * Figures closing the block.
   */
  protected readonly figures = computed(() => {
    const consistency = this.consistency();
    return consistency ? buildConsistencyFigures(consistency, this.translator()) : [];
  });

  /**
   * Match under the pointer, or `null`.
   */
  protected readonly hovered = signal<ChartTooltipAnchor | null>(null);

  /**
   * Content of the tooltip, or `null` while nothing is hovered.
   */
  protected readonly tooltip = computed(() => {
    const dot = this.dots()[this.hovered()?.index ?? -1];
    return dot
      ? buildConsistencyTooltip(dot, this.translator(), this.translation.language())
      : null;
  });

  /**
   * Prose standing in for the plot, read out to assistive technology.
   */
  protected readonly summary = computed(() =>
    this.figures()
      .map((figure) => `${figure.caption} : ${figure.value} (${figure.detail}).`)
      .join(' '),
  );

  /**
   * Draws the chart once the canvas exists, and again whenever the selection or the language
   * changes.
   */
  constructor() {
    registerChartComponents();

    effect(() => {
      const canvas = this.canvas()?.nativeElement;
      const consistency = this.consistency();
      const axis = this.axis();
      this.translation.language();
      if (canvas && consistency && axis) {
        untracked(() => this.draw(canvas, consistency, axis));
      }
    });

    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  /**
   * Builds the chart in place of the previous one.
   *
   * @param canvas - Canvas to paint on.
   * @param consistency - Spread to draw.
   * @param axis - Axis of the chart.
   */
  private draw(
    canvas: HTMLCanvasElement,
    consistency: ConsistencySummary,
    axis: ConsistencyAxis,
  ): void {
    const translate = this.translator();
    const theme = resolveChartTheme();
    const dots = this.dots();
    const amber = resolveSeriesColor(0);

    this.chart?.destroy();
    this.chart = new Chart<'scatter', ConsistencyDot[]>(canvas, {
      type: 'scatter',
      data: {
        datasets: [
          {
            data: [...dots],
            backgroundColor: this.dotColors(dots),
            borderWidth: 0,
            pointRadius: (context) => this.dotRadius(context),
            pointHoverRadius: (context) => this.dotRadius(context) + 2,
            pointHoverBorderWidth: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        interaction: { mode: 'nearest', intersect: true },
        layout: { padding: { top: 24 } },
        parsing: false,
        scales: {
          x: {
            type: 'linear',
            min: axis.min,
            max: axis.max,
            grid: { display: false },
            border: { color: theme.grid },
            title: axisTitleOptions(theme, translate(`${CONSISTENCY_I18N}.xAxis`)),
            ticks: {
              color: theme.tick,
              stepSize: CONSISTENCY_AXIS_STEP,
              maxRotation: 0,
              font: AXIS_TICK_FONT,
            },
          },
          y: {
            min: 0,
            max: resolveConsistencyStackHeight(dots),
            grid: { color: theme.grid },
            border: { display: false },
            title: axisTitleOptions(theme, translate(`${CONSISTENCY_I18N}.yAxis`)),
            ticks: { color: theme.tick, maxTicksLimit: 6, precision: 0, font: AXIS_TICK_FONT },
          },
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            enabled: false,
            external: trackChartTooltip<'scatter'>((anchor) => this.hovered.set(anchor)),
          },
        },
      },
      plugins: [
        createConsistencyBandPlugin(
          () => consistency,
          () => ({
            floor: translate(`${CONSISTENCY_I18N}.floorRule`, {
              value: Math.round(consistency.floor),
            }),
            ceiling: translate(`${CONSISTENCY_I18N}.ceilingRule`, {
              value: Math.round(consistency.ceiling),
            }),
          }),
          amber,
        ),
      ],
    });
  }

  /**
   * Colours each dot: amber inside the floor-to-ceiling zone, a quiet grey outside it.
   *
   * @param dots - Dots of the season shown.
   * @returns One fill per dot.
   */
  private dotColors(dots: readonly ConsistencyDot[]): string[] {
    const amber = resolveSeriesColor(0);
    return dots.map((dot) => (dot.zone === 'inside' ? amber : CONSISTENCY_OUTSIDE_COLOR));
  }

  /**
   * Sizes a dot to the room its column and its stack level get, so neighbours never touch.
   *
   * @param context - Chart.js scripting context of the dot.
   * @returns The dot radius, in pixels.
   */
  private dotRadius(context: ScriptableContext<'line'>): number {
    const { chartArea, scales } = context.chart;
    if (!chartArea) {
      return 4;
    }
    const axis = this.axis();
    if (!axis) {
      return 4;
    }
    const column =
      ((chartArea.right - chartArea.left) * CONSISTENCY_BIN_WIDTH) / (axis.max - axis.min);
    const level = (chartArea.bottom - chartArea.top) / Number(scales['y'].max);
    return Math.max(3, Math.min(column, level) * CONSISTENCY_DOT_FILL);
  }

  /**
   * Dictionary lookup bound to the translation service.
   *
   * @returns The lookup handed to the pure helpers.
   */
  private translator(): TranslateFn {
    this.translation.language();
    return (key, params) => this.translation.translate(key, params);
  }
}
