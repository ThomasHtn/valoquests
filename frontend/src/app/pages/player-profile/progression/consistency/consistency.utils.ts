import { Plugin } from 'chart.js';

import { formatCampaignDayMonth } from '@core/date/date-format.utils';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { resolveMatchScore } from '@core/matches/display/match-format.utils';
import { formatScore } from '@core/players/player-format.utils';
import { ConsistencySummary } from '@core/players/progression/player-progression.model';
import { formatSeasonName } from '@core/seasons/season-name.utils';
import { AXIS_TICK_FONT } from '@shared/chart/chart-theme.constants';

import { KeyFigure } from '../key-figures/key-figures.model';
import {
  CONSISTENCY_AXIS_STEP,
  CONSISTENCY_BIN_WIDTH,
  CONSISTENCY_I18N as KEYS,
  CONSISTENCY_MIN_STACK,
  CONSISTENCY_RESULT_MODIFIERS,
  CONSISTENCY_RULE_COLOR,
  CONSISTENCY_SMALL_STACK,
  CONSISTENCY_STEADY_MARGIN,
  CONSISTENCY_TREND_ICONS,
  CONSISTENCY_TREND_TONES,
} from './consistency.constants';
import {
  ConsistencyAxis,
  ConsistencyBandLabels,
  ConsistencyDot,
  ConsistencyTooltip,
  ConsistencyTrend,
  ConsistencyZone,
} from './consistency.model';

/**
 * Combat-score axis: round ends around the scores, columns starting on the lowest.
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
 * Where a combat score falls against the season's floor and ceiling.
 */
export function resolveConsistencyZone(acs: number, season: ConsistencySummary): ConsistencyZone {
  if (acs < season.floor) {
    return 'below';
  }
  return acs > season.ceiling ? 'above' : 'inside';
}

/**
 * Stacks the matches into their combat-score columns, lowest score first.
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
 * Top of the vertical axis, in matches: tallest stack plus one free level, rounded to the
 * Chart.js tick step (2 up to ten, 5 beyond) so the axis ends on a labelled line.
 */
export function resolveConsistencyStackHeight(dots: readonly ConsistencyDot[]): number {
  const tallest = Math.max(0, ...dots.map((dot) => dot.y + 0.5)) + 1;
  const step = tallest > CONSISTENCY_SMALL_STACK ? 5 : 2;
  return Math.max(CONSISTENCY_MIN_STACK, Math.ceil(tallest / step) * step);
}

/**
 * Figures under the chart: floor, median, ceiling, sample, and the trend when one applies.
 */
export function buildConsistencyFigures(
  summary: ConsistencySummary,
  translate: TranslateFn,
): readonly KeyFigure[] {
  const figures: KeyFigure[] = [
    {
      caption: translate(`${KEYS}.figures.floor`),
      value: formatScore(summary.floor),
      valueTone: 'var(--color-brand-500)',
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
      valueTone: 'var(--color-brand-500)',
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
    figures.push(
      buildTrendFigure(summary, summary.previousSpread, summary.previousSeasonName, translate),
    );
  }
  return figures;
}

/**
 * Trend figure: tighter, looser or steady spread against the previous season.
 */
function buildTrendFigure(
  summary: ConsistencySummary,
  previousSpread: number,
  previousSeasonName: string,
  translate: TranslateFn,
): KeyFigure {
  const trend = resolveSpreadTrend(summary.spread, previousSpread);
  return {
    caption: translate(`${KEYS}.figures.trend`),
    value: translate(`${KEYS}.figures.${trend}`),
    detail: translate(`${KEYS}.figures.trendDetail`, {
      spread: formatScore(summary.spread),
      previous: formatScore(previousSpread),
      season: formatSeasonName(previousSeasonName, translate),
    }),
    icon: CONSISTENCY_TREND_ICONS[trend],
    tone: CONSISTENCY_TREND_TONES[trend],
  };
}

/**
 * Compares two spreads, a change within the steady margin counting as no change.
 */
function resolveSpreadTrend(spread: number, previousSpread: number): ConsistencyTrend {
  // No previous spread to divide by: steady.
  const ratio = previousSpread > 0 ? spread / previousSpread : 1;
  if (ratio < 1 - CONSISTENCY_STEADY_MARGIN) {
    return 'tighter';
  }
  if (ratio > 1 + CONSISTENCY_STEADY_MARGIN) {
    return 'looser';
  }
  return 'steady';
}

/**
 * Tooltip of one match.
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
    resultClass: CONSISTENCY_RESULT_MODIFIERS[match.result] ?? '',
    zone: dot.zone,
    zoneLabel: translate(`${KEYS}.zone.${dot.zone}`),
    mapName: match.mapName,
    agentName: match.agentName,
    date: formatCampaignDayMonth(match.startedAt, language),
  };
}

/**
 * Plugin drawing the floor and ceiling as dashed rules, captioned above the plot.
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
