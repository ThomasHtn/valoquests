import { resolveCssColor } from '@shared/chart/chart-theme';
import { ChartGaugeSegment } from '@shared/chart/chart.model';

/**
 * Tailwind utility of a turnout pip that cleared the threshold.
 */
export const PRESENCE_PIP_FULL_CLASS = 'bg-accent-cyan';

/**
 * Tailwind utility of a turnout pip that played, but under the threshold.
 */
export const PRESENCE_PIP_PARTIAL_CLASS = 'bg-accent-cyan/30';

/**
 * Colour of the food ring's segment for the most recent day of the window — the one still open,
 * closing at tonight's reset. The one segment the ring must draw attention to, which is worth a
 * colour no other day on the ring wears.
 */
export const FOOD_SEGMENT_LAST_DAY_COLOR = 'var(--color-danger)';

/**
 * Colour of the food ring's segment for the day actually being lived, when that is not also the
 * last day of the window — a live marker distinct from the countdown red, since the two can land on
 * different segments once today's harvest has not posted yet.
 */
export const FOOD_SEGMENT_TODAY_COLOR = 'var(--color-brand-400)';

/**
 * Colour of the food ring's segment for a day nobody played, or not yet lived.
 */
export const FOOD_SEGMENT_EMPTY_COLOR =
  'color-mix(in oklab, var(--color-brand-500) 15%, transparent)';

/**
 * Colour of a played day, brighter the closer its harvest is to the window's best day.
 *
 * The ring used to carry this as the pod's own opacity; the conic-gradient segment reads the same
 * share as a colour-mix percentage instead, so a strong evening still stands out among duller ones
 * without the ring needing a second visual channel.
 *
 * @param percentage - Share of the window's best day, in `[0, 100]`.
 * @returns The `color-mix` expression the segment's `conic-gradient` stop is drawn in.
 */
export function foodSegmentPlayedColor(percentage: number): string {
  const alpha = 30 + percentage * 0.5;

  return `color-mix(in oklab, var(--color-brand-500) ${alpha}%, transparent)`;
}

/**
 * Fill of a half-donut gauge's unfilled share, shared by every gauge on the campaign page and the
 * colony resource band — a half-empty gauge reads the same wherever it is drawn.
 */
export const GAUGE_TRACK_COLOR = 'color-mix(in oklab, var(--color-text-primary) 12%, transparent)';

/**
 * The turnout gauge's own arcs: one per roster cell, lit clockwise from the left by tonight's head
 * count.
 *
 * Shared by `ColonyResourceBand`'s own presence dome and the campaign page's compact Participation
 * tile, so the same roster reads as the same gauge wherever it is drawn.
 *
 * @param cells - One flag per roster cell, lit when that cell's player turned up tonight.
 * @returns The gauge's own arcs, in draw order.
 */
export function presenceGaugeSegments(cells: readonly boolean[]): ChartGaugeSegment[] {
  const lit = resolveCssColor('var(--color-accent-cyan)');
  const track = resolveCssColor(GAUGE_TRACK_COLOR);

  return cells.map((isLit) => ({ value: 1, color: isLit ? lit : track, label: '' }));
}

/**
 * The attractivity gauge's own arcs: morale itself against the ceiling it climbs towards.
 *
 * Shared by `ColonyResourceBand`'s own attractivity dome and the campaign page's compact Moral
 * tile.
 *
 * @param percentage - Where the fill ends: morale itself, out of its ceiling, in `[0, 100]`.
 * @param label - Already-translated reading of the fill, shown in each arc's own tooltip.
 * @returns The gauge's own arcs, in draw order.
 */
export function attractivityGaugeSegments(percentage: number, label: string): ChartGaugeSegment[] {
  const fill = resolveCssColor('var(--color-accent-violet)');
  const track = resolveCssColor(GAUGE_TRACK_COLOR);

  return [
    { value: percentage, color: fill, label },
    { value: 100 - percentage, color: track, label },
  ];
}
