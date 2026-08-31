import { describe, expect, it } from 'vitest';

import { buildFriezeStars, railSplitPercentage } from './campaign-frieze.utils';

describe('railSplitPercentage', () => {
  it('splits at the centre of the week being fought', () => {
    // Ten cells of equal width put marker 3 at (3 + 0.5) / 10.
    expect(railSplitPercentage(3, 10)).toBe(35);
  });

  it('leaves the whole rail solid once every week has settled', () => {
    expect(railSplitPercentage(-1, 10)).toBe(100);
  });

  it('leaves it empty while the run has no week yet', () => {
    expect(railSplitPercentage(-1, 0)).toBe(0);
  });
});

describe('buildFriezeStars', () => {
  it('draws the same field on every call, so the sky never shuffles between renders', () => {
    expect(buildFriezeStars()).toEqual(buildFriezeStars());
  });

  it('keeps every star inside the plot box', () => {
    for (const star of buildFriezeStars()) {
      expect(star.x).toBeGreaterThanOrEqual(0);
      expect(star.x).toBeLessThanOrEqual(1000);
      expect(star.y).toBeGreaterThanOrEqual(0);
      expect(star.y).toBeLessThanOrEqual(400);
      expect(star.o).toBeGreaterThan(0);
      expect(star.o).toBeLessThanOrEqual(0.5);
    }
  });
});
