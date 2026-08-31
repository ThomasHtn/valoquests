import { describe, expect, it } from 'vitest';

import { ColonyMilestone, ColonyTrajectoryPoint } from './colony.model';
import { buildRunCurve } from './run-curve.utils';

function point(runDay: number, population: number): ColonyTrajectoryPoint {
  return {
    day: `2026-08-${String(runDay).padStart(2, '0')}`,
    runDay,
    population,
    feedablePopulation: population * 2,
    efficiency: 3,
    materials: 100 * runDay,
    foodStock: 500,
    morale: 60,
    presenceCount: 4,
  };
}

function milestone(runDay: number): ColonyMilestone {
  return { name: 'HAMLET', level: 1, day: '2026-08-09', runDay, threshold: 100 };
}

describe('buildRunCurve', () => {
  it('returns nothing while the run has no day to plot', () => {
    expect(buildRunCurve([], [], 71, () => 'Hameau')).toBeNull();
  });

  it('spans the whole run, not only the days played', () => {
    const curve = buildRunCurve([point(1, 0), point(8, 50)], [], 71, () => 'Hameau');

    // Day 8 of 71 is a tenth of the way in: the curve has to stop there rather than fill the box,
    // which is what tells a run four days old from one nearly over.
    expect(curve?.todayPercentage).toBe(10);
    expect(curve?.remainingWidth).toBe(900);
    expect(curve?.linePath).toBe('M0.0,240.0 L100.0,66.1');
  });

  it('closes the area on the baseline under the first and last day', () => {
    const curve = buildRunCurve([point(1, 10), point(8, 40)], [], 71, () => 'Hameau');

    expect(curve?.areaPath.endsWith('L100.0,240 L0.0,240 Z')).toBe(true);
  });

  it('draws a tick on every week boundary of the run', () => {
    const curve = buildRunCurve([point(1, 10)], [], 22, () => 'Hameau');

    // Days 8, 15 and 22 of a 22-day run.
    expect(curve?.weekTicks).toEqual([333.3, 666.7, 1000]);
  });

  it('pins a milestone to the population of the day it was crossed', () => {
    const curve = buildRunCurve(
      [point(1, 0), point(8, 50), point(15, 100)],
      [milestone(8)],
      71,
      () => 'Hameau',
    );

    expect(curve?.milestones).toEqual([
      { name: 'Hameau', x: 100, y: 153, leftPercentage: 10, topPercentage: 58.86 },
    ]);
  });

  it('keeps a run that has housed nobody flat on the baseline', () => {
    const curve = buildRunCurve([point(1, 0), point(2, 0)], [], 71, () => 'Hameau');

    expect(curve?.linePath).toBe('M0.0,240.0 L14.3,240.0');
  });
});
