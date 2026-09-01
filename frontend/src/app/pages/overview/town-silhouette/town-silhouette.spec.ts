import { describe, expect, it } from 'vitest';

import { phaseAt } from './town-silhouette';

/*
 * The boundaries are the whole of this decision, and an off-by-one at 7 or at 21 is invisible in a
 * screenshot taken at any other time of day.
 */
describe('phaseAt', () => {
  it('runs the day in order, dawn to night', () => {
    expect(phaseAt(8)).toBe('dawn');
    expect(phaseAt(14)).toBe('day');
    expect(phaseAt(19)).toBe('dusk');
    expect(phaseAt(23)).toBe('night');
  });

  it('changes at the hour it says it does', () => {
    expect(phaseAt(6)).toBe('night');
    expect(phaseAt(7)).toBe('dawn');
    expect(phaseAt(9)).toBe('dawn');
    expect(phaseAt(10)).toBe('day');
    expect(phaseAt(17)).toBe('day');
    expect(phaseAt(18)).toBe('dusk');
    expect(phaseAt(20)).toBe('dusk');
    expect(phaseAt(21)).toBe('night');
  });

  it('leaves the small hours to the night rather than to a default', () => {
    expect(phaseAt(0)).toBe('night');
    expect(phaseAt(3)).toBe('night');
  });
});
