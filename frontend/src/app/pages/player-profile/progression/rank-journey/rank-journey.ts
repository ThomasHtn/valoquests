import { NgOptimizedImage } from '@angular/common';
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
import {
  LucideArrowDownUp,
  LucideMinus,
  LucideSwords,
  LucideTrendingDown,
  LucideTrendingUp,
  LucideTrophy,
} from '@lucide/angular';
import { Chart } from 'chart.js';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TranslateFn } from '@core/i18n/translation.model';
import {
  resolveCompetitiveTierVisual,
  resolveTierFromOrdinal,
  resolveTierOrdinal,
} from '@core/players/competitive-tier.utils';
import { SeasonRank } from '@core/players/player-progression.model';
import { ChartTooltip } from '@shared/chart/chart-tooltip';
import { trackChartTooltip } from '@shared/chart/chart-tooltip.utils';
import { createCrosshairPlugin } from '@shared/chart/chart-plugins.utils';
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
  RANK_JOURNEY_COMPACT_QUERY,
  RANK_JOURNEY_COMPACT_SIZES,
  RANK_JOURNEY_I18N,
  RANK_JOURNEY_WIDE_SIZES,
} from './rank-journey.constants';
import { RankJourneySeries } from './rank-journey.model';
import {
  buildRankJourneyFigures,
  buildRankJourneySeries,
  buildRankJourneyTooltip,
  createRankPeakPlugin,
  createRankRailPlugin,
  loadRankBadge,
  resolveRankColor,
} from './rank-journey.utils';

/**
 * Where the selected seasons left a player on the ladder: one rank badge per season, or, for a
 * single season, the rank match by match.
 *
 * Read per season when there are several: a match-by-match line across many acts spends most of
 * its width bouncing between two divisions, while the season's final rank is the one Riot keeps.
 */
@Component({
  selector: 'app-rank-journey',
  imports: [
    NgOptimizedImage,
    TranslatePipe,
    Tooltip,
    ChartTooltip,
    KeyFigures,
    LucideArrowDownUp,
    LucideMinus,
    LucideSwords,
    LucideTrendingDown,
    LucideTrendingUp,
    LucideTrophy,
  ],
  templateUrl: './rank-journey.html',
})
export class RankJourney {
  /**
   * Selected seasons, oldest first.
   */
  public readonly journey = input.required<readonly SeasonRank[]>();

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
  private chart: Chart<'line'> | null = null;

  /**
   * Increases with every draw request, so a draw still waiting for its badges to load gives way
   * to a newer one.
   */
  private drawRequest = 0;

  /**
   * Point under the pointer, or `null`.
   */
  protected readonly hovered = signal<ChartTooltipAnchor | null>(null);

  /**
   * Line currently plotted, or `null` before the first draw.
   */
  private readonly series = signal<RankJourneySeries | null>(null);

  /**
   * Prefix of the translation keys the template reads.
   */
  protected readonly keys = RANK_JOURNEY_I18N;

  /**
   * Figures closing the block.
   */
  protected readonly figures = computed(() =>
    this.journey().length > 0 ? buildRankJourneyFigures(this.journey(), this.translator()) : [],
  );

  /**
   * Content of the tooltip, or `null` while nothing is hovered.
   */
  protected readonly tooltip = computed(() => {
    const anchor = this.hovered();
    const series = this.series();
    return anchor && series
      ? buildRankJourneyTooltip(this.journey(), series, anchor.index, this.translator())
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
   * Draws the chart once the canvas exists, and again whenever the seasons or the language change.
   */
  constructor() {
    registerChartComponents();

    effect(() => {
      const canvas = this.canvas()?.nativeElement;
      const journey = this.journey();
      this.translation.language();
      if (canvas && journey.length > 0) {
        untracked(() => void this.draw(canvas, journey));
      }
    });

    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  /**
   * Loads the rank badges, then builds the chart in place of the previous one.
   *
   * @param canvas - Canvas to paint on.
   * @param journey - Seasons to plot, never empty.
   */
  private async draw(canvas: HTMLCanvasElement, journey: readonly SeasonRank[]): Promise<void> {
    const request = ++this.drawRequest;
    const sizes = window.matchMedia(RANK_JOURNEY_COMPACT_QUERY).matches
      ? RANK_JOURNEY_COMPACT_SIZES
      : RANK_JOURNEY_WIDE_SIZES;
    const translate = this.translator();
    const series = buildRankJourneySeries(journey, sizes === RANK_JOURNEY_COMPACT_SIZES, translate);
    const perSeason = series.mode === 'seasons';
    const lastIndex = series.tiers.length - 1;
    // A season carries its badge on every point; a match line only on its last one.
    const badges = series.tiers.map((tier, index) =>
      perSeason || index === lastIndex ? loadRankBadge(tier, sizes.point) : null,
    );
    const peakBadge = loadRankBadge(series.peakTier, sizes.peak);
    await Promise.all(
      [...badges, peakBadge].map((image) => image?.decode().catch(() => undefined)),
    );
    if (request !== this.drawRequest) {
      return;
    }

    const theme = resolveChartTheme();
    const amber = resolveSeriesColor(0);
    this.hovered.set(null);
    this.series.set(series);

    this.chart?.destroy();
    this.chart = new Chart<'line'>(canvas, {
      type: 'line',
      data: {
        labels: [...series.labels],
        datasets: [
          {
            data: series.tiers.map(resolveTierOrdinal),
            borderColor: amber,
            backgroundColor: amber,
            borderWidth: 2,
            tension: 0,
            stepped: perSeason ? false : 'after',
            fill: false,
            // The peak point carries the enlarged badge from the peak plugin instead.
            pointStyle: badges.map((badge, index) =>
              index === series.peakIndex && series.peakOnPoint ? false : (badge ?? 'circle'),
            ),
            pointRadius: badges.map((badge) => (badge ? sizes.point / 2 : 0)),
            pointHoverRadius: badges.map((badge) => (badge ? sizes.point / 2 : 4)),
            pointHoverBorderWidth: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        devicePixelRatio: chartPixelRatio(),
        animation: prefersReducedMotion() ? false : { duration: 400 },
        interaction: { mode: 'index', intersect: false },
        layout: { padding: { top: 8, right: 12, left: 4 } },
        scales: {
          x: {
            offset: perSeason,
            grid: { display: false },
            border: { color: theme.grid },
            title: axisTitleOptions(
              theme,
              perSeason ? '' : translate(`${RANK_JOURNEY_I18N}.axisMatch`),
            ),
            ticks: {
              color: theme.tick,
              maxRotation: 0,
              autoSkip: !perSeason,
              maxTicksLimit: perSeason ? undefined : 8,
              font: AXIS_TICK_FONT,
            },
          },
          y: {
            min: series.lowestOrdinal - 1,
            max: series.highestOrdinal + 1,
            grid: { color: theme.grid },
            border: { display: false },
            ticks: {
              stepSize: 1,
              font: AXIS_TICK_FONT,
              color: (context) => resolveRankColor(resolveTierFromOrdinal(context.tick.value)),
              callback: (value) => {
                const tier = resolveTierFromOrdinal(Number(value));
                return tier ? resolveCompetitiveTierVisual(tier, translate).label : '';
              },
            },
          },
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            enabled: false,
            external: trackChartTooltip<'line'>((anchor) => this.hovered.set(anchor)),
          },
        },
      },
      plugins: [
        ...(perSeason ? [createRankRailPlugin(journey, sizes)] : []),
        createCrosshairPlugin(theme),
        createRankPeakPlugin(
          series,
          peakBadge,
          {
            rank: resolveCompetitiveTierVisual(series.peakTier, translate).label,
            color: resolveRankColor(series.peakTier),
            caption: translate(`${RANK_JOURNEY_I18N}.peak`),
          },
          theme,
        ),
      ],
    });
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
