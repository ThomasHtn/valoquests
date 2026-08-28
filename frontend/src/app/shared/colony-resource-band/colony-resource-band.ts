import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import {
  LucideGauge,
  LucideMagnet,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
} from '@lucide/angular';

import { attractivityGaugeSegments, presenceGaugeSegments } from '@core/colony/colony-gauge.utils';
import {
  ColonyAttractivityView,
  ColonyBatteryView,
  ColonyDeltaView,
  ColonyFoodRingView,
  ColonyPresencePipView,
} from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolveCssColor } from '@shared/chart/chart-theme';
import { ChartGaugeSegment } from '@shared/chart/chart.model';
import { HalfDonutChart } from '@shared/chart/half-donut-chart';
import { Tooltip } from '@shared/tooltip/tooltip';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';

/**
 * How far the seat inside the population hexagon is scaled down from the outline around it, so the
 * outline reads as a rim of even thickness on every side.
 */
const HEXAGON_INNER_SCALE = 0.95;

/**
 * One gauge's own geometry and type sizes, at one of the band's two densities.
 */
interface GaugeMetrics {
  /**
   * Width of the gauge's own box, in `rem` — a half-circle's diameter.
   */
  readonly widthRem: number;

  /**
   * Height of the gauge's own box, in `rem` — a half-circle's radius, give or take, the exact
   * figure tuned by eye against `cutout` so the arc fills its box without excess padding.
   */
  readonly heightRem: number;

  /**
   * Share of the gauge's own radius left hollow at its centre, handed straight to Chart.js.
   */
  readonly cutout: string;

  /**
   * Size of the identifying glyph seated in the gauge's own hollow — decoration only, so it stays
   * small and carries no figure of its own; the reading lives below, where there is room to be read
   * at a glance instead of guessed at.
   */
  readonly hubIcon: string;

  /**
   * Size of the bold headline figure below the gauge — the one number the gauge exists to show.
   */
  readonly headline: string;
}

/**
 * Everything that changes between the band's two densities.
 *
 * Kept as one table rather than a ternary per element: the two densities are two sizes of the same
 * block, and spreading that decision over a dozen inline conditions is how the compact version
 * drifted from the full one in the first place.
 *
 * `ring` is the food dome, the band's lead figure; `satellite` is shared by the presence dome and
 * the attractivity dome, the two smaller gauges drawn either side of it. `podiumHeightRem` is the
 * food dome's own height, reused as every gauge's outer seat so the three sit on one shared ground
 * line regardless of their own size — without it the satellites, shorter than the food dome, centred
 * on their own height instead and every reading below them landed at its own, different level.
 */
interface ColonyBandMetrics {
  readonly hexagonSeat: string;
  readonly hexagon: string;
  readonly hexagonIcon: string;
  readonly hexagonFigure: string;
  readonly hexagonDelta: string;

  readonly podiumHeightRem: number;
  readonly ring: GaugeMetrics;
  readonly satellite: GaugeMetrics;
}

/**
 * The band at full size: a page of its own to stand in.
 *
 * Sized `176 × 203`, the `1 : 1.1547` box `clip-hex` needs to come out a regular hexagon. On a
 * square box the clip came out squat, and the fill inside it read against a silhouette a seventh
 * shorter than every other hexagon on the page.
 */
const COMFORTABLE_METRICS: ColonyBandMetrics = {
  hexagonSeat: 'flex shrink-0 items-center justify-center',
  hexagon:
    'focus-ring relative h-[12.7rem] w-44 shrink-0 cursor-pointer transition-transform ' +
    'duration-200 hover:scale-[1.04] motion-reduce:transition-none',
  hexagonIcon: 'size-5',
  hexagonFigure: 'text-3xl',
  hexagonDelta: 'text-xs',

  // The food dome is the band's centrepiece, drawn a size larger than the satellites either side of
  // it so the row reads as one lead figure with two smaller readings, not three equal gauges.
  podiumHeightRem: 6.1,
  ring: {
    widthRem: 11.5,
    heightRem: 6.1,
    cutout: '62%',
    hubIcon: 'size-6',
    headline: 'text-2xl',
  },
  satellite: {
    widthRem: 8.5,
    heightRem: 4.5,
    cutout: '58%',
    hubIcon: 'size-5',
    headline: 'text-xl',
  },
};

/**
 * The same band in half a column, where it is a summary of a page rather than the page itself.
 *
 * Sized `120 × 139`, the same `1 : 1.1547` box the hexagon needs. Nothing is dropped: the compact
 * band shows the same three gauges, only smaller and without the readings that live under a
 * pointer.
 */
const COMPACT_METRICS: ColonyBandMetrics = {
  hexagonSeat: 'flex shrink-0 items-center justify-center',
  hexagon: 'relative block h-[8.66rem] w-30 shrink-0',
  hexagonIcon: 'size-4',
  hexagonFigure: 'text-2xl',
  hexagonDelta: 'text-2xs',

  podiumHeightRem: 3.95,
  ring: {
    widthRem: 7.4,
    heightRem: 3.95,
    cutout: '60%',
    hubIcon: 'size-4',
    headline: 'text-base',
  },
  satellite: {
    widthRem: 5.6,
    heightRem: 2.95,
    cutout: '56%',
    hubIcon: 'size-3.5',
    headline: 'text-sm',
  },
};

/**
 * The run's standing figures: the population it scores, and the three mechanisms that set it.
 *
 * One block for both screens that state where the run stands — the campaign page, which owns it, and
 * the overview, which summarizes it. The population hexagon is the score; the presence dome, the food
 * dome and the attractivity dome are each a cause of it — a head count, a stock and a speed — drawn as
 * one family of `app-half-donut-chart` gauges. Chart.js, the same library the rest of the site plots
 * curves and bars with, rather than a hand-rolled `conic-gradient`/`clip-path` stack, which is what
 * drew every curve here before and never anti-aliased as cleanly as a canvas does.
 *
 * Every gauge keeps its own reading out of its hollow: the hollow holds an identifying glyph only,
 * and the headline figure, its caption and the line explaining what it is built from all sit below the
 * arc, where the box actually has the width to set them apart — cramming all three into the hollow is
 * what made an early pass of this band unreadable.
 *
 * Purely presentational: every figure, colour and sentence arrives resolved from `ColonyView`, so the
 * band only positions them. The one exception is colour resolution itself: Chart.js paints on a
 * canvas, which cannot read a `var()` or an unresolved `color-mix()`, so every colour a gauge's own
 * arcs are given passes through {@link resolveCssColor} first.
 *
 * Hosted as `display: contents` — the seats become flex items of whatever the caller lays them out
 * in, which is what lets the campaign page seat its run ledger on the same row.
 */
@Component({
  selector: 'app-colony-resource-band',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    HalfDonutChart,
    Tooltip,
    LucideGauge,
    LucideMagnet,
    LucideUserCheck,
    LucideUsers,
    LucideWheat,
  ],
  templateUrl: './colony-resource-band.html',
  host: { class: 'contents' },
})
export class ColonyResourceBand {
  /**
   * Already-formatted population, the run's score.
   */
  public readonly populationLabel = input.required<string>();

  /**
   * Share of the housing the population already fills, which is how high the hexagon is filled.
   */
  public readonly populationPercentage = input.required<number>();

  /**
   * What the night moved, raised on the figure it moved. `null` while the run has not resolved.
   */
  public readonly delta = input<ColonyDeltaView | null>(null);

  /**
   * Accessible name of the population hexagon, which carries in one sentence everything the shape
   * says. Only read when the band is interactive, the hexagon being a plain figure otherwise.
   */
  public readonly hexagonAriaLabel = input('');

  /**
   * The turnout dome's own data — one cell lit per player tonight's turnout has cleared. `null`
   * while the run has not resolved.
   */
  public readonly battery = input<ColonyBatteryView | null>(null);

  /**
   * One pip per player of the roster, for the presence dome's hover card. Never drawn on a compact
   * band, which has no hover cards.
   */
  public readonly presencePips = input<readonly ColonyPresencePipView[]>([]);

  /**
   * The food ring. `null` while the run has not resolved.
   */
  public readonly foodRing = input<ColonyFoodRingView | null>(null);

  /**
   * The attractivity dome's own data. `null` while the run has not resolved.
   */
  public readonly attractivity = input<ColonyAttractivityView | null>(null);

  /**
   * Whether the band is drawn small and inert.
   *
   * One input for both, deliberately: the compact band exists to sit inside a link covering the whole
   * summary, and a link may hold no button — so a compact band that opened cards on its own would be
   * invalid markup wherever it is actually used.
   */
  public readonly compact = input(false);

  /**
   * Asks the host to open the population curve, from the hexagon carrying its current value. Never
   * emitted by a compact band.
   */
  public readonly curveOpen = output<void>();

  /**
   * Scale the template applies to the seat inside the population hexagon.
   */
  protected readonly hexagonInnerScale = HEXAGON_INNER_SCALE;

  /**
   * Silhouette the hover cards borrow from the sidebar's tooltips, so a surface floating over the
   * page reads the same wherever it comes from.
   */
  protected readonly tooltipSurfaceClass = TOOLTIP_SURFACE_CLASS;

  /**
   * The sizes this band is drawn at.
   */
  protected readonly metrics = computed<ColonyBandMetrics>(() =>
    this.compact() ? COMPACT_METRICS : COMFORTABLE_METRICS,
  );

  /**
   * The presence dome's own arcs: one per roster cell, lit clockwise from the left by tonight's head
   * count — the same cells the battery used to fill bottom-up, read here around a gauge instead.
   */
  protected readonly presenceSegments = computed<readonly ChartGaugeSegment[]>(() =>
    presenceGaugeSegments(this.battery()?.cells ?? []),
  );

  /**
   * The food dome's own arcs: one per day, every one the same weight regardless of what it holds so
   * the gauge always reads as an even week rather than the days with the biggest harvests swallowing
   * it. Each arc's own colour, already resolved by `ColonyView`, is what marks out the day actually
   * being lived from the last day of the window closing at tonight's reset.
   */
  protected readonly foodSegments = computed<readonly ChartGaugeSegment[]>(() => {
    const days = this.foodRing()?.days ?? [];

    return days.map((day) => ({
      value: 1,
      color: resolveCssColor(day.segmentColor),
      label: day.ariaLabel,
    }));
  });

  /**
   * The attractivity dome's own arcs: morale itself against the ceiling it climbs towards, the same
   * fill/track split every other progress indicator in the app reads.
   */
  protected readonly attractivitySegments = computed<readonly ChartGaugeSegment[]>(() => {
    const attractivity = this.attractivity();

    return attractivity === null
      ? []
      : attractivityGaugeSegments(attractivity.percentage, attractivity.moraleLabel);
  });

  /**
   * Text colour of the arrival mark, by the direction the night moved — the same rule the
   * hexagon's own exponent is coloured by.
   *
   * @param delta - What the night moved.
   * @returns The colour utility.
   */
  protected deltaColorClass(delta: ColonyDeltaView): string {
    return delta.isPositive ? 'text-success' : delta.isNegative ? 'text-danger' : 'text-text-muted';
  }
}
