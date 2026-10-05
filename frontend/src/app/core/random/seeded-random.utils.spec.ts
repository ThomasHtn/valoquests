import { describe, expect, it } from 'vitest';

import { createSeededRandom } from './seeded-random.utils';

describe('createSeededRandom', () => {
  it('replays the same sequence for the same seed', () => {
    const first = createSeededRandom(42);
    const second = createSeededRandom(42);

    expect([first(), first(), first()]).toEqual([second(), second(), second()]);
  });

  it('yields a different sequence for a different seed', () => {
    expect(createSeededRandom(1)()).not.toBe(createSeededRandom(2)());
  });

  it('stays within [0, 1)', () => {
    const random = createSeededRandom(20260905);
    for (let i = 0; i < 1000; i++) {
      const value = random();
      expect(value).toBeGreaterThanOrEqual(0);
      expect(value).toBeLessThan(1);
    }
  });

  it('stays within [0, 1) for a negative or fractional seed', () => {
    for (const seed of [-1, -20260905, 0.5, -7.25]) {
      const random = createSeededRandom(seed);
      for (let i = 0; i < 100; i++) {
        const value = random();
        expect(value).toBeGreaterThanOrEqual(0);
        expect(value).toBeLessThan(1);
      }
    }
  });

  it('keeps the sequence of a positive integer seed', () => {
    expect(createSeededRandom(42)()).toBe((42 * 1664525 + 1013904223) / 4294967296);
  });
});
