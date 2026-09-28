import { describe, expect, it } from 'vitest';

import { hashUnit } from './hash-unit.utils';

describe('hashUnit', () => {
  it('gives the same draw for the same keys', () => {
    expect(hashUnit(3, 7, 11)).toBe(hashUnit(3, 7, 11));
  });

  it('depends on the order of the keys', () => {
    expect(hashUnit(1, 2)).not.toBe(hashUnit(2, 1));
  });

  it('stays within [0, 1) and spreads over the range', () => {
    const draws = Array.from({ length: 2000 }, (_, i) => hashUnit(i, 42));
    expect(Math.min(...draws)).toBeGreaterThanOrEqual(0);
    expect(Math.max(...draws)).toBeLessThan(1);
    const low = draws.filter((d) => d < 0.5).length;
    expect(low).toBeGreaterThan(900);
    expect(low).toBeLessThan(1100);
  });
});
