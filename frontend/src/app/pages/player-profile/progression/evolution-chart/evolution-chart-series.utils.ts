import {
  ProgressionMatchPoint,
  SeasonEvolution,
} from '@core/players/progression/player-progression.model';
import { ChartSeries } from '@shared/chart/chart.model';
import { EvolutionMetric } from './evolution-chart.model';

/**
 * Curves of one metric; with several seasons, shorter ones are front-padded with `null` so every
 * season ends on the same abscissa and end states compare. Layout only, no value is computed.
 */
export function buildEvolutionSeries(
  evolution: readonly SeasonEvolution[],
  metric: EvolutionMetric,
  colorOf: (seasonId: number) => string,
): readonly ChartSeries[] {
  const longest = evolution.reduce((length, season) => Math.max(length, season.points.length), 0);

  return evolution.map((season) => ({
    label: season.seasonName,
    color: colorOf(season.seasonId),
    points: [
      ...Array<number | null>(longest - season.points.length).fill(null),
      ...season.points.map((point) => readMetric(point, metric)),
    ],
  }));
}

/**
 * One metric of a plotted match, `null` for a mode that did not report it.
 */
export function readMetric(point: ProgressionMatchPoint, metric: EvolutionMetric): number | null {
  return point[metric];
}
