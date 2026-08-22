import { ChartSeries } from '@shared/chart/chart.model';
import { ProgressionMatchPoint, SeasonEvolution } from '@core/players/player-progression.model';

/**
 * One of the metrics the evolution chart can plot.
 *
 * Swapped rather than stacked: headshot rate, K/D, ACS and ADR have four different units, and
 * plotting two of them together would need a second y-axis — the one chart construction that
 * reliably makes a reader draw the wrong conclusion.
 */
export type EvolutionMetric = 'headshotPercentage' | 'kda' | 'acs' | 'adr';

/**
 * Every metric, in the order the swap buttons offer them.
 */
export const EVOLUTION_METRICS: readonly EvolutionMetric[] = [
  'headshotPercentage',
  'kda',
  'acs',
  'adr',
];

/**
 * Builds the curves for one metric, ending every season on the same abscissa.
 *
 * With one season selected the curve is simply that season's matches, first to last. With several,
 * the axis is as long as the longest season and the shorter ones are pushed right by padding their
 * front with `null`s — so the reader compares each season's *end state*, which is where a season
 * left the player, rather than lining up match 1 of a finished season against match 1 of one that
 * is three weeks old.
 *
 * This is layout, not statistics: no value is computed, invented or reordered here.
 *
 * @param evolution - Per-season series, as the API returned them.
 * @param metric - The metric to plot.
 * @param colorOf - Resolves a season's color from its identifier.
 * @returns One curve per season, padded to a common length.
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
 * Reads one metric off a plotted match.
 *
 * @param point - The match to read.
 * @param metric - The metric to read.
 * @returns The value, or `null` for a mode that did not report it.
 */
export function readMetric(point: ProgressionMatchPoint, metric: EvolutionMetric): number | null {
  return point[metric];
}
