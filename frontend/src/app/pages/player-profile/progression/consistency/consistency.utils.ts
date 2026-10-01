import { Plugin } from 'chart.js';

import { formatLocalDayMonth } from '@core/date/date-time.utils';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { formatSeasonName } from '@core/matches/season-name.utils';
import { resolveMatchScore } from '@core/matches/match-format.utils';
import { formatScore } from '@core/players/player-format.utils';
import { ConsistencySummary } from '@core/players/player-progression.model';
import { AXIS_TICK_FONT } from '@shared/chart/chart-theme.constants';
import { KeyFigure } from '../key-figures/key-figures.model';
import {
  CONSISTENCY_AXIS_STEP,
  CONSISTENCY_BIN_WIDTH,
  CONSISTENCY_I18N as KEYS,
  CONSISTENCY_MIN_STACK,
  CONSISTENCY_SMALL_STACK,
  CONSISTENCY_RESULT_CLASSES,
  CONSISTENCY_RESULT_FALLBACK_CLASS,
  CONSISTENCY_RULE_COLOR,
  CONSISTENCY_STEADY_MARGIN,
} from './consistency.constants';
import {
  ConsistencyAxis,
  ConsistencyBandLabels,
  ConsistencyDot,
  ConsistencyTooltip,
  ConsistencyZone,
} from './consistency.model';

/**
 * Builds the combat-score axis: round ends around the scores, and a grid of columns starting on
 * the lowest one.
 *
 * @param summary - The spread to draw, never empty.
 * @returns The axis ends and the column grid.
 */
export function buildConsistencyAxis(summary: ConsistencySummary): ConsistencyAxis {
  const scores = summary.matches.map((match) => match.acs);
  const lowest = Math.min(...scores);
  const highest = Math.max(...scores);
  const max = Math.floor(highest / CONSISTENCY_AXIS_STEP + 1) * CONSISTENCY_AXIS_STEP;
  const binOrigin = Math.floor(lowest / CONSISTENCY_BIN_WIDTH) * CONSISTENCY_BIN_WIDTH;

  return {
    min: Math.floor(lowest / CONSISTENCY_AXIS_STEP) * CONSISTENCY_AXIS_STEP,
    max,
    binOrigin,
    binCount: Math.ceil((max - binOrigin) / CONSISTENCY_BIN_WIDTH),
  };
}

/**
 * Places where a combat score falls against a floor and a ceiling.
 *
 * @param acs - The combat score.
 * @param season - The spread it belongs to.
 * @returns Below the floor, inside the zone, or above the ceiling.
 */
export function resolveConsistencyZone(acs: number, season: ConsistencySummary): ConsistencyZone {
  if (acs < season.floor) {
    return 'below';
  }
  return acs > season.ceiling ? 'above' : 'inside';
}

/**
 * Stacks a spread's matches into their combat-score columns, lowest score first.
 *
 * @param season - The spread to draw.
 * @param axis - The shared axis.
 * @returns One dot per match.
 */
export function stackConsistencyDots(
  season: ConsistencySummary,
  axis: ConsistencyAxis,
): readonly ConsistencyDot[] {
  const heights = new Array<number>(axis.binCount).fill(0);
  return [...season.matches]
    .sort((left, right) => left.acs - right.acs)
    .map((match) => {
      const column = Math.min(
        axis.binCount - 1,
        Math.floor((match.acs - axis.binOrigin) / CONSISTENCY_BIN_WIDTH),
      );
      heights[column] += 1;
      return {
        x: axis.binOrigin + (column + 0.5) * CONSISTENCY_BIN_WIDTH,
        y: heights[column] - 0.5,
        zone: resolveConsistencyZone(match.acs, season),
        match,
      };
    });
}

/**
 * Top of the vertical axis for one entry: its tallest stack plus a free level, rounded to the step
 * Chart.js picks for its ticks (2 up to ten, 5 beyond), so the axis ends on a labelled line.
 *
 * @param dots - The entry's dots.
 * @returns The axis maximum, in matches.
 */
export function resolveConsistencyStackHeight(dots: readonly ConsistencyDot[]): number {
  const tallest = Math.max(0, ...dots.map((dot) => dot.y + 0.5)) + 1;
  const step = tallest > CONSISTENCY_SMALL_STACK ? 5 : 2;
  return Math.max(CONSISTENCY_MIN_STACK, Math.ceil(tallest / step) * step);
}

/**
 * Builds the strip of figures under the chart: floor, median, ceiling, the sample, and the trend
 * against the previous season when the selection is a single season that has one.
 *
 * @param summary - The spread shown.
 * @param translate - Dictionary lookup.
 * @returns The key figures, in reading order.
 */
export function buildConsistencyFigures(
  summary: ConsistencySummary,
  translate: TranslateFn,
): readonly KeyFigure[] {
  const figures: KeyFigure[] = [
    {
      caption: translate(`${KEYS}.figures.floor`),
      value: formatScore(summary.floor),
      valueClass: 'text-brand-500',
      detail: translate(`${KEYS}.figures.floorDetail`),
      icon: 'floor',
    },
    {
      caption: translate(`${KEYS}.figures.median`),
      value: formatScore(summary.median),
      detail: translate(`${KEYS}.figures.medianDetail`),
      icon: 'median',
      tone: 'neutral',
    },
    {
      caption: translate(`${KEYS}.figures.ceiling`),
      value: formatScore(summary.ceiling),
      valueClass: 'text-brand-500',
      detail: translate(`${KEYS}.figures.ceilingDetail`),
      icon: 'ceiling',
    },
    {
      caption: translate(`${KEYS}.figures.matches`),
      value: String(summary.matches.length),
      detail: translate(
        `${KEYS}.figures.${summary.seasonCount > 1 ? 'matchesDetail' : 'matchesDetailOne'}`,
        { count: summary.seasonCount },
      ),
      icon: 'matches',
      tone: 'neutral',
    },
  ];

  if (summary.previousSpread !== null && summary.previousSeasonName !== null) {
    const ratio = summary.previousSpread > 0 ? summary.spread / summary.previousSpread : 1;
    const trend =
      ratio < 1 - CONSISTENCY_STEADY_MARGIN
        ? 'tighter'
        : ratio > 1 + CONSISTENCY_STEADY_MARGIN
          ? 'looser'
          : 'steady';
    figures.push({
      caption: translate(`${KEYS}.figures.trend`),
      value: translate(`${KEYS}.figures.${trend}`),
      detail: translate(`${KEYS}.figures.trendDetail`, {
        spread: formatScore(summary.spread),
        previous: formatScore(summary.previousSpread),
        season: formatSeasonName(summary.previousSeasonName, translate),
      }),
      icon: trend === 'steady' ? 'flat' : trend,
      tone: trend === 'tighter' ? 'good' : trend === 'looser' ? 'bad' : 'neutral',
    });
  }
  return figures;
}

/**
 * Builds the tooltip of one match.
 *
 * @param dot - The hovered dot.
 * @param translate - Dictionary lookup.
 * @param language - Active language, for the date.
 * @returns The tooltip content.
 */
export function buildConsistencyTooltip(
  dot: ConsistencyDot,
  translate: TranslateFn,
  language: Language,
): ConsistencyTooltip {
  const { match } = dot;
  const score = resolveMatchScore(match.allyScore, match.enemyScore);
  const outcome = translate(`playerProfile.matches.result.${match.result}`);
  return {
    acs: Math.round(match.acs),
    result: score ? `${outcome} ${score.ally}-${score.enemy}` : outcome,
    resultClass: CONSISTENCY_RESULT_CLASSES[match.result] ?? CONSISTENCY_RESULT_FALLBACK_CLASS,
    zone: dot.zone,
    zoneLabel: translate(`${KEYS}.zone.${dot.zone}`),
    mapName: match.mapName,
    agentName: match.agentName,
    date: formatLocalDayMonth(match.startedAt, language),
  };
}

/**
 * Builds the plugin drawing the floor and ceiling as dashed rules, captioned above the plot.
 *
 * @param season - Returns the spread shown.
 * @param labels - Returns the captions of both rules.
 * @param color - Colour of the captions.
 * @returns The band plugin.
 */
export function createConsistencyBandPlugin(
  season: () => ConsistencySummary | null,
  labels: () => ConsistencyBandLabels,
  color: string,
): Plugin<'scatter'> {
  return {
    id: 'consistencyBand',
    beforeDatasetsDraw(chart) {
      const shown = season();
      if (!shown) {
        return;
      }

      const { ctx, chartArea, scales } = chart;
      const captions = labels();
      ctx.save();
      ctx.font = `600 ${AXIS_TICK_FONT.size}px ${AXIS_TICK_FONT.family}`;
      ctx.textBaseline = 'bottom';
      const rules: readonly [number, string, CanvasTextAlign, number][] = [
        [shown.floor, captions.floor, 'right', -5],
        [shown.ceiling, captions.ceiling, 'left', 5],
      ];
      for (const [value, caption, align, offset] of rules) {
        const x = scales['x'].getPixelForValue(value);
        ctx.setLineDash([4, 4]);
        ctx.lineWidth = 1;
        ctx.strokeStyle = CONSISTENCY_RULE_COLOR;
        ctx.beginPath();
        ctx.moveTo(x, chartArea.top);
        ctx.lineTo(x, chartArea.bottom);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.fillStyle = color;
        ctx.textAlign = align;
        ctx.fillText(caption, x + offset, chartArea.top - 6);
      }
      ctx.restore();
    },
  };
}
