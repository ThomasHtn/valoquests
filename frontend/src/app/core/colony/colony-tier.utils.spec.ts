import { describe, expect, it } from 'vitest';

import { tierGlyphFor, tierShareOfGain, tierStepFor } from './colony-tier.utils';

describe('tierGlyphFor', () => {
  it('sorts the ladder into five bands, never one glyph per name', () => {
    expect(tierGlyphFor({ name: 'CAMP' })).toBe('CAMP');
    expect(tierGlyphFor({ name: 'VILLAGE' })).toBe('HOUSES');
    expect(tierGlyphFor({ name: 'CITY' })).toBe('SKYLINE');
    expect(tierGlyphFor({ name: 'CITADEL' })).toBe('MONUMENT');
    expect(tierGlyphFor({ name: 'ECUMENOPOLIS' })).toBe('SPRAWL');
  });
});

describe('tierStepFor', () => {
  it('numbers the named steps in the order the ladder climbs them', () => {
    expect(tierStepFor({ name: 'CAMP', level: 0 })).toBe(0);
    expect(tierStepFor({ name: 'HAMLET', level: 0 })).toBe(1);
    expect(tierStepFor({ name: 'CAPITAL', level: 0 })).toBe(10);
  });

  it('names every step a run can reach, past the citadel', () => {
    expect(tierStepFor({ name: 'CITADEL', level: 0 })).toBe(11);
    expect(tierStepFor({ name: 'CONURBATION', level: 0 })).toBe(12);
    expect(tierStepFor({ name: 'CONTINUUM', level: 0 })).toBe(16);
  });

  it('folds the number of the last name back into the step, so the ladder stays open-ended', () => {
    expect(tierStepFor({ name: 'STRATUM', level: 1 })).toBe(17);
    expect(tierStepFor({ name: 'STRATUM', level: 2 })).toBe(18);
    expect(tierStepFor({ name: 'STRATUM', level: 7 })).toBe(23);
  });

  it('reads an unnumbered last name as the first of its kind', () => {
    expect(tierStepFor({ name: 'STRATUM', level: 0 })).toBe(17);
  });
});

describe('tierShareOfGain', () => {
  /*
   * The run's own numbers on the day this was written: the step from Campement to Hameau spans
   * 8,75 − 8,00 of efficiency, and week two's standard boss is worth 0,533 of it.
   */
  const CAMP = 8;
  const HAMLET = 8.75;

  it('reads a fight as the share of the step its materials cover', () => {
    expect(tierShareOfGain(0.5333333333333332, CAMP, HAMLET)).toBeCloseTo(71.11, 2);
    expect(tierShareOfGain(0.375, CAMP, HAMLET)).toBe(50);
  });

  it('says so when one fight is worth more than the step still to climb', () => {
    expect(tierShareOfGain(1.5, CAMP, HAMLET)).toBe(200);
  });

  it('has nothing to measure against a step of no width', () => {
    expect(tierShareOfGain(0.5, HAMLET, HAMLET)).toBeNull();
    expect(tierShareOfGain(0.5, HAMLET, CAMP)).toBeNull();
  });

  it('has nothing to draw for a fight worth no efficiency', () => {
    expect(tierShareOfGain(0, CAMP, HAMLET)).toBeNull();
  });
});
