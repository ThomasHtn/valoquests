import { Plugin } from 'chart.js';

import { TranslateFn } from '@core/i18n/translation.model';
import { formatSeasonName, splitSeasonName } from '@core/matches/season-name.utils';
import { CompetitiveTier } from '@core/players/competitive-tier.model';
import {
  resolveCompetitiveTierColorVariable,
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
  resolveTierOrdinal,
} from '@core/players/competitive-tier.utils';
import { formatWinRate } from '@core/players/player-format.utils';
import { SeasonRank } from '@core/players/player-progression.model';
import { AXIS_TICK_FONT } from '@shared/chart/chart-theme.constants';
import { ChartTheme } from '@shared/chart/chart-theme.model';
import { token } from '@shared/chart/chart-theme.utils';
import { KeyFigure } from '../key-figures/key-figures.model';
import {
  RANK_JOURNEY_I18N as KEYS,
  RANK_JOURNEY_RAIL_COLOR,
  RANK_JOURNEY_RAIL_OVERHANG,
} from './rank-journey.constants';
import {
  RankDelta,
  RankJourneyIconSizes,
  RankJourneySeries,
  RankJourneyTooltip,
  RankLabel,
} from './rank-journey.model';

/**
 * Index of the season holding the career peak: the first one to reach the highest rank.
 *
 * @param journey - Seasons, oldest first, never empty.
 * @returns Index of the peak season.
 */
export function findPeakSeasonIndex(journey: readonly SeasonRank[]): number {
  let peak = 0;
  journey.forEach((season, index) => {
    if (resolveTierOrdinal(season.highestTier) > resolveTierOrdinal(journey[peak].highestTier)) {
      peak = index;
    }
  });
  return peak;
}

/**
 * Formats the move between two ranks, counted in divisions.
 *
 * @param from - Rank moved from.
 * @param to - Rank moved to.
 * @param translate - Dictionary lookup.
 * @returns The move with its arrow and tone.
 */
export function describeRankDelta(
  from: CompetitiveTier,
  to: CompetitiveTier,
  translate: TranslateFn,
): RankDelta {
  const delta = resolveTierOrdinal(to) - resolveTierOrdinal(from);
  if (delta === 0) {
    return { label: translate(`${KEYS}.stable`), icon: 'flat', tone: 'neutral' };
  }

  const count = Math.abs(delta);
  const label = translate(`${KEYS}.${count > 1 ? 'divisions' : 'division'}`, {
    sign: delta > 0 ? '+' : '−',
    count,
  });
  return delta > 0
    ? { label, icon: 'trendUp', tone: 'good' }
    : { label, icon: 'trendDown', tone: 'bad' };
}

/**
 * Builds the axis label of every season: its act, with the era underneath the first act of each
 * era only, so a row of eight acts does not repeat the same episode eight times.
 *
 * @param journey - Seasons, oldest first.
 * @param compact - Whether to use the short act label of the phone layout.
 * @param translate - Dictionary lookup.
 * @returns One two-line label per season.
 */
export function buildRankJourneyAxisLabels(
  journey: readonly SeasonRank[],
  compact: boolean,
  translate: TranslateFn,
): string[][] {
  let previousEra: string | null = null;
  return journey.map((season) => {
    const parts = splitSeasonName(season.seasonName, translate);
    if (!parts) {
      previousEra = null;
      return [season.seasonName, ''];
    }

    const act = translate(`${KEYS}.${compact ? 'axisActShort' : 'axisAct'}`, { act: parts.act });
    const era = parts.era === previousEra ? '' : parts.era;
    previousEra = parts.era;
    return [act, era];
  });
}

/**
 * Builds the plotted line: one point per season for a multi-season selection, one point per
 * ranked match when the selection is a single season, where a point per season would be a lone dot.
 *
 * @param journey - Selected seasons, oldest first, never empty.
 * @param compact - Whether to use the short act labels of the phone layout.
 * @param translate - Dictionary lookup.
 * @returns The line to plot.
 */
export function buildRankJourneySeries(
  journey: readonly SeasonRank[],
  compact: boolean,
  translate: TranslateFn,
): RankJourneySeries {
  if (journey.length === 1) {
    const tiers = journey[0].rankedTiers;
    const ordinals = tiers.map(resolveTierOrdinal);
    const peakIndex = ordinals.indexOf(Math.max(...ordinals));
    return {
      mode: 'matches',
      tiers,
      labels: tiers.map((_, index) => String(index + 1)),
      peakIndex,
      peakTier: tiers[peakIndex],
      peakOnPoint: true,
      lowestOrdinal: Math.min(...ordinals),
      highestOrdinal: Math.max(...ordinals),
    };
  }

  const peakIndex = findPeakSeasonIndex(journey);
  const peakTier = journey[peakIndex].highestTier;
  return {
    mode: 'seasons',
    tiers: journey.map((season) => season.finalTier),
    labels: buildRankJourneyAxisLabels(journey, compact, translate),
    peakIndex,
    peakTier,
    peakOnPoint: journey[peakIndex].finalTier === peakTier,
    lowestOrdinal: Math.min(...journey.map((season) => resolveTierOrdinal(season.lowestTier))),
    highestOrdinal: Math.max(...journey.map((season) => resolveTierOrdinal(season.highestTier))),
  };
}

/**
 * Builds the strip of figures under the chart: the peak, where the selection ended, and the climb
 * since its start.
 *
 * @param journey - Selected seasons, oldest first, never empty.
 * @param translate - Dictionary lookup.
 * @returns The key figures, in reading order.
 */
export function buildRankJourneyFigures(
  journey: readonly SeasonRank[],
  translate: TranslateFn,
): readonly KeyFigure[] {
  const single = journey.length === 1;
  const peak = journey[findPeakSeasonIndex(journey)];
  const first = journey[0];
  const last = journey[journey.length - 1];
  const startTier = single ? first.rankedTiers[0] : first.finalTier;
  const peakVisual = resolveCompetitiveTierVisual(peak.highestTier, translate);
  const lastVisual = resolveCompetitiveTierVisual(last.finalTier, translate);
  const climb = describeRankDelta(startTier, last.finalTier, translate);

  return [
    {
      caption: translate(`${KEYS}.figures.peak`),
      value: peakVisual.label,
      valueClass: peakVisual.colorClass,
      detail: seasonLabel(peak.seasonName, translate),
      rankIconUrl: resolveCompetitiveTierIconUrl(peak.highestTier),
    },
    {
      caption: translate(`${KEYS}.figures.${single ? 'final' : 'lastSeason'}`),
      value: lastVisual.label,
      valueClass: lastVisual.colorClass,
      detail: seasonLabel(last.seasonName, translate),
      rankIconUrl: resolveCompetitiveTierIconUrl(last.finalTier),
    },
    {
      caption: translate(`${KEYS}.figures.evolution`),
      value: climb.label,
      detail: single
        ? translate(`${KEYS}.figures.sinceStart`)
        : translate(`${KEYS}.figures.since`, { season: seasonLabel(first.seasonName, translate) }),
      icon: climb.icon,
      tone: climb.tone,
    },
  ];
}

/**
 * Builds the tooltip of one point: a season with its range and record, or a match with the rank
 * it left the player on.
 *
 * @param journey - Selected seasons, oldest first.
 * @param series - The plotted line.
 * @param index - Index of the hovered point.
 * @param translate - Dictionary lookup.
 * @returns The tooltip content, or `null` when the index is out of range.
 */
export function buildRankJourneyTooltip(
  journey: readonly SeasonRank[],
  series: RankJourneySeries,
  index: number,
  translate: TranslateFn,
): RankJourneyTooltip | null {
  const tier = series.tiers[index];
  if (!tier) {
    return null;
  }

  const previous = series.tiers[index - 1];
  const common = {
    tier: rankLabel(tier, translate),
    tierClass: resolveCompetitiveTierVisual(tier, translate).colorClass,
    delta: previous ? describeRankDelta(previous, tier, translate) : null,
  };

  if (series.mode === 'matches') {
    return {
      ...common,
      heading: translate(`${KEYS}.tip.match`, { index: index + 1, total: series.tiers.length }),
      caption: translate(`${KEYS}.tip.afterMatch`),
      deltaCaption: translate(`${KEYS}.tip.deltaMatch`),
      lowest: null,
      highest: null,
      matchesPlayed: null,
      wins: null,
    };
  }

  const season = journey[index];
  const spansSeveralRanks = season.highestTier !== season.lowestTier;
  return {
    ...common,
    heading: seasonLabel(season.seasonName, translate),
    caption: translate(`${KEYS}.tip.finalTier`),
    deltaCaption: translate(`${KEYS}.tip.delta`),
    lowest: spansSeveralRanks ? rankLabel(season.lowestTier, translate) : null,
    highest: spansSeveralRanks ? rankLabel(season.highestTier, translate) : null,
    matchesPlayed: season.matchesPlayed,
    wins: translate(`${KEYS}.tip.winsValue`, {
      wins: season.wins,
      rate: formatWinRate(
        season.matchesPlayed > 0 ? (season.wins / season.matchesPlayed) * 100 : null,
      ),
    }),
  };
}

/**
 * Builds the plugin drawing, behind the line, each season's rail from its lowest to its highest
 * rank.
 *
 * @param journey - Seasons, oldest first.
 * @param sizes - Breakpoint sizes, read for the rail width.
 * @returns The rail plugin.
 */
export function createRankRailPlugin(
  journey: readonly SeasonRank[],
  sizes: RankJourneyIconSizes,
): Plugin<'line'> {
  return {
    id: 'rankRails',
    beforeDatasetsDraw(chart) {
      const { ctx, scales } = chart;
      ctx.save();
      ctx.fillStyle = RANK_JOURNEY_RAIL_COLOR;
      journey.forEach((season, index) => {
        const x = scales['x'].getPixelForValue(index);
        const top = scales['y'].getPixelForValue(
          resolveTierOrdinal(season.highestTier) + RANK_JOURNEY_RAIL_OVERHANG,
        );
        const bottom = scales['y'].getPixelForValue(
          resolveTierOrdinal(season.lowestTier) - RANK_JOURNEY_RAIL_OVERHANG,
        );
        ctx.beginPath();
        ctx.roundRect(x - sizes.rail / 2, top, sizes.rail, bottom - top, sizes.rail / 2);
        ctx.fill();
      });
      ctx.restore();
    },
  };
}

/**
 * Builds the plugin drawing the peak: its badge enlarged on the point that reached it, with the
 * rank named beside it in the rank's own colour.
 *
 * @param series - The plotted line.
 * @param icon - Loaded badge of the peak rank.
 * @param labels - Peak rank name, its colour and the caption under it.
 * @param theme - Resolved chart palette.
 * @returns The peak plugin.
 */
export function createRankPeakPlugin(
  series: RankJourneySeries,
  icon: HTMLImageElement,
  labels: { readonly rank: string; readonly color: string; readonly caption: string },
  theme: ChartTheme,
): Plugin<'line'> {
  const peakOrdinal = resolveTierOrdinal(series.peakTier);

  return {
    id: 'rankPeak',
    afterDatasetsDraw(chart) {
      const { ctx, scales, chartArea } = chart;
      const x = scales['x'].getPixelForValue(series.peakIndex);
      const y = scales['y'].getPixelForValue(peakOrdinal);
      const size = icon.width;
      const onRight = x < chartArea.right - 120;
      const textX = x + (onRight ? 1 : -1) * (size / 2 + 8);

      ctx.save();
      ctx.drawImage(icon, x - size / 2, y - size / 2, size, size);
      ctx.textAlign = onRight ? 'left' : 'right';
      ctx.textBaseline = 'middle';
      ctx.font = `600 15px ${AXIS_TICK_FONT.family}`;
      ctx.fillStyle = labels.color;
      ctx.fillText(labels.rank, textX, y - 8);
      ctx.font = `${AXIS_TICK_FONT.size}px ${AXIS_TICK_FONT.family}`;
      ctx.fillStyle = theme.tick;
      ctx.fillText(labels.caption, textX, y + 9);
      ctx.restore();
    },
  };
}

/**
 * Names a rank with its badge.
 *
 * @param tier - The rank.
 * @param translate - Dictionary lookup.
 * @returns The rank's label and badge.
 */
function rankLabel(tier: CompetitiveTier, translate: TranslateFn): RankLabel {
  return {
    label: resolveCompetitiveTierVisual(tier, translate).label,
    iconUrl: resolveCompetitiveTierIconUrl(tier),
  };
}

/**
 * Spells a season's raw code out.
 *
 * @param name - Raw season code.
 * @param translate - Dictionary lookup.
 * @returns The season's full name.
 */
function seasonLabel(name: string, translate: TranslateFn): string {
  return formatSeasonName(name, translate);
}

/**
 * Starts loading a rank badge at the size it is drawn on the canvas.
 *
 * @param tier - Rank whose badge to load.
 * @param size - Edge of the drawn badge, in pixels.
 * @returns The image, loading.
 */
export function loadRankBadge(tier: CompetitiveTier, size: number): HTMLImageElement {
  const image = new Image(size, size);
  image.src = resolveCompetitiveTierIconUrl(tier) ?? '';
  return image;
}

/**
 * Resolves the colour a rank is written in on the canvas, the same one its badge text uses.
 *
 * @param tier - The rank, or `null` past either end of the axis.
 * @returns A CSS colour.
 */
export function resolveRankColor(tier: CompetitiveTier | null): string {
  return tier ? token(resolveCompetitiveTierColorVariable(tier), '#868b8d') : 'transparent';
}
