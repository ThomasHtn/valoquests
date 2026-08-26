import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import {
  LucideFrown,
  LucideMeh,
  LucideSmile,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
} from '@lucide/angular';

import {
  ColonyDeltaView,
  ColonyPresencePipView,
  ColonyTrackView,
} from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';

/**
 * How far the seat inside the population hexagon is scaled down from the outline around it, so the
 * outline reads as a rim of even thickness on every side.
 */
const HEXAGON_INNER_SCALE = 0.95;

/**
 * Same, for a rail's glyph socket.
 *
 * A separate figure because the rim has to keep its apparent thickness on a socket a quarter the
 * size of the population hexagon: at the hexagon's own scale it came out under a pixel and
 * disappeared.
 */
const SOCKET_INNER_SCALE = 0.84;

/**
 * Everything that changes between the band's two densities.
 *
 * Kept as one table rather than a ternary per element: the two densities are two sizes of the same
 * block, and spreading that decision over a dozen inline conditions is how the compact version
 * drifted from the full one in the first place.
 */
interface ColonyBandMetrics {
  readonly hexagonSeat: string;
  readonly hexagon: string;
  readonly hexagonIcon: string;
  readonly hexagonFigure: string;
  readonly hexagonDelta: string;
  readonly rails: string;
  readonly socket: string;
  readonly socketGlyph: string;
  readonly barHeight: string;
  readonly value: string;
}

/**
 * The band at full size: a page of its own to stand in.
 *
 * Sized `176 × 203`, the `1 : 1.1547` box `clip-hex` needs to come out a regular hexagon. On a
 * square box the clip came out squat, and the fill inside it read against a silhouette a seventh
 * shorter than every other hexagon on the page.
 */
const COMFORTABLE_METRICS: ColonyBandMetrics = {
  hexagonSeat: 'flex shrink-0 items-center justify-center lg:w-[11rem]',
  hexagon:
    'focus-ring relative h-[12.7rem] w-44 shrink-0 cursor-pointer transition-transform ' +
    'duration-200 hover:scale-[1.04] motion-reduce:transition-none',
  hexagonIcon: 'size-5',
  hexagonFigure: 'text-3xl',
  hexagonDelta: 'text-xs',
  rails: 'flex min-w-0 flex-1 flex-col justify-center gap-1 lg:ml-6',
  socket: 'size-10',
  socketGlyph: '[&_svg]:size-4',
  barHeight: 'h-3.5',
  value: 'w-[7.5rem] text-lg',
};

/**
 * The same band in half a column, where it is a summary of a page rather than the page itself.
 *
 * Sized `120 × 139`, the same `1 : 1.1547` box. Nothing is dropped: the compact band shows the same
 * hexagon and the same three rails, only smaller and without the readings that live under a pointer.
 */
const COMPACT_METRICS: ColonyBandMetrics = {
  hexagonSeat: 'flex shrink-0 items-center justify-center',
  hexagon: 'relative block h-[8.66rem] w-30 shrink-0',
  hexagonIcon: 'size-4',
  hexagonFigure: 'text-2xl',
  hexagonDelta: 'text-2xs',
  rails: 'flex min-w-0 flex-1 flex-col justify-center gap-1',
  socket: 'size-8',
  socketGlyph: '[&_svg]:size-3.5',
  barHeight: 'h-2.5',
  value: 'w-[5.5rem] text-sm',
};

/**
 * The run's standing figures: the population it scores, and the three rails that set it.
 *
 * One block for both screens that state where the run stands — the campaign page, which owns it, and
 * the overview, which summarizes it. The population hexagon is the score, and every rail beside it is
 * either feeding that figure or spending towards it; drawn twice from two sets of markup, the two
 * screens had already drifted into two different readings of one run.
 *
 * Purely presentational: every figure, colour and sentence arrives resolved from `ColonyView`, so the
 * band only positions them.
 *
 * Hosted as `display: contents` — the hexagon and the rails become flex items of whatever the caller
 * lays them out in, which is what lets the campaign page seat its run ledger on the same row.
 */
@Component({
  selector: 'app-colony-resource-band',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    ProgressBar,
    LucideFrown,
    LucideMeh,
    LucideSmile,
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
   * The rails, food first.
   */
  public readonly tracks = input.required<readonly ColonyTrackView[]>();

  /**
   * One pip per player of the roster, for the turnout rail's hover card. Never drawn on a compact
   * band, which has no hover cards.
   */
  public readonly presencePips = input<readonly ColonyPresencePipView[]>([]);

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
   * Scales the template applies to the seat inside a hexagon.
   */
  protected readonly hexagonInnerScale = HEXAGON_INNER_SCALE;
  protected readonly socketInnerScale = SOCKET_INNER_SCALE;

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
}
