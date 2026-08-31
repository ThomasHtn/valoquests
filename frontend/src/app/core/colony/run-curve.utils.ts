import { ColonyMilestone, ColonyTrajectoryPoint } from './colony.model';

/**
 * Drawing box of the run curve, in user units.
 *
 * The plot is stretched to its panel (`preserveAspectRatio="none"`), so these numbers never reach a
 * screen — they only have to give the path enough resolution to stay smooth once stretched. The
 * baseline sits above the bottom edge so the area's own outline is not clipped by it, and the top
 * margin is what keeps the peak from touching the frame.
 */
const VIEW_WIDTH = 1000;
const VIEW_HEIGHT = 260;
const BASELINE_Y = 240;
const PLOT_HEIGHT = 200;

/**
 * Headroom above the run's peak, so the highest point of a curve still climbing does not sit on the
 * frame — a line that ends against the top edge reads as clipped rather than as leading.
 */
const PEAK_HEADROOM = 1.15;

/**
 * Days in a week, the interval the vertical ticks are drawn on: the run is counted in weeks
 * everywhere else on the page, so the grid behind the curve is too.
 */
const DAYS_PER_WEEK = 7;

/**
 * One step of the ladder, placed on the curve at the day the town crossed it.
 *
 * Carries both the user-unit position (for the SVG dot) and the percentage one (for the HTML label
 * riding over the plot): the label is a real text node so it stays legible and selectable, and the
 * plot it sits on is stretched, so it cannot be positioned in the same units as the dot.
 */
export interface RunCurveMilestone {
  readonly name: string;
  readonly x: number;
  readonly y: number;
  readonly leftPercentage: number;
  readonly topPercentage: number;
}

/**
 * The run's population curve, resolved into everything the template draws.
 *
 * Built here rather than handed to the shared `app-line-chart`: this plot carries three things a
 * chart component has no notion of — the veil over the days not lived yet, the marker on today, and
 * the ladder steps pinned to the line at the day they were crossed. All three are what make the
 * curve say "the run is a third done" instead of "the population went up".
 */
export interface RunCurveView {
  readonly linePath: string;
  readonly areaPath: string;

  /**
   * Vertical rules behind the curve, one per week boundary, in user units.
   */
  readonly weekTicks: readonly number[];

  /**
   * Where the days played stop, in user units and as a share of the width — the veil over what is
   * left starts there, and so does the "today" rule.
   */
  readonly todayX: number;
  readonly todayPercentage: number;

  /**
   * Width of that veil, in user units, so the template does not have to subtract in the markup.
   */
  readonly remainingWidth: number;

  /**
   * The last point of the line, marked with a dot — the run's own head.
   */
  readonly lastX: number;
  readonly lastY: number;

  readonly milestones: readonly RunCurveMilestone[];
}

/**
 * Resolves a day of the run to its horizontal position.
 *
 * Day one sits on the left edge and the run's last day on the right one, so the width is the run
 * itself and never the days played: a curve rescaled to what has been lived would look identical on
 * day 4 and on day 70, which is the one thing this plot exists to tell apart.
 *
 * @param runDay - Day of the run, from one.
 * @param runDayCount - How many days the run holds.
 * @returns The x position, in user units.
 */
function xFor(runDay: number, runDayCount: number): number {
  const span = Math.max(1, runDayCount - 1);
  return ((runDay - 1) / span) * VIEW_WIDTH;
}

/**
 * Resolves a population to its vertical position.
 *
 * @param population - Inhabitants on that day.
 * @param ceiling - Population the plot's top edge stands for.
 * @returns The y position, in user units.
 */
function yFor(population: number, ceiling: number): number {
  return BASELINE_Y - (population / ceiling) * PLOT_HEIGHT;
}

/**
 * Builds the run's curve from the trajectory the backend replayed.
 *
 * @param points - The run's days, oldest first, only as far as it has been played.
 * @param milestones - Days the town crossed a step of its ladder, with the step's display name.
 * @param runDayCount - How many days the whole run holds, played or not.
 * @param nameFor - Resolves a milestone to its already-translated step name.
 * @returns The resolved curve, or `null` when there is nothing to plot yet.
 */
export function buildRunCurve(
  points: readonly ColonyTrajectoryPoint[],
  milestones: readonly ColonyMilestone[],
  runDayCount: number,
  nameFor: (milestone: ColonyMilestone) => string,
): RunCurveView | null {
  if (points.length === 0) {
    return null;
  }

  // A run that has not housed anyone yet would divide by zero, and its flat line belongs on the
  // baseline rather than halfway up an arbitrary scale.
  const peak = points.reduce((highest, point) => Math.max(highest, point.population), 0);
  const ceiling = peak > 0 ? peak * PEAK_HEADROOM : 1;

  const coordinates = points.map((point) => ({
    x: xFor(point.runDay, runDayCount),
    y: yFor(point.population, ceiling),
  }));

  const linePath = coordinates
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(' ');

  const first = coordinates[0];
  const last = coordinates[coordinates.length - 1];
  const areaPath = `${linePath} L${last.x.toFixed(1)},${BASELINE_Y} L${first.x.toFixed(1)},${BASELINE_Y} Z`;

  const weekTicks: number[] = [];
  for (let day = DAYS_PER_WEEK + 1; day <= runDayCount; day += DAYS_PER_WEEK) {
    weekTicks.push(Number(xFor(day, runDayCount).toFixed(1)));
  }

  return {
    linePath,
    areaPath,
    weekTicks,
    todayX: Number(last.x.toFixed(1)),
    todayPercentage: Number(((last.x / VIEW_WIDTH) * 100).toFixed(2)),
    remainingWidth: Number((VIEW_WIDTH - last.x).toFixed(1)),
    lastX: Number(last.x.toFixed(1)),
    lastY: Number(last.y.toFixed(1)),
    milestones: milestones.map((milestone) => {
      const x = xFor(milestone.runDay, runDayCount);
      const population =
        points.find((point) => point.runDay === milestone.runDay)?.population ?? peak;
      const y = yFor(population, ceiling);

      return {
        name: nameFor(milestone),
        x: Number(x.toFixed(1)),
        y: Number(y.toFixed(1)),
        leftPercentage: Number(((x / VIEW_WIDTH) * 100).toFixed(2)),
        topPercentage: Number(((y / VIEW_HEIGHT) * 100).toFixed(2)),
      };
    }),
  };
}
