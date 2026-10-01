import { describe, expect, it } from 'vitest';

import { formatSeasonName, splitSeasonName } from './season-name.utils';

/**
 * Stands in for the translation service, echoing the key and its parameters so the assertions read
 * on the substitution rather than on a dictionary's wording.
 */
function translate(key: string, params?: Readonly<Record<string, string | number>>): string {
  return `${key}(${Object.entries(params ?? {})
    .map(([name, value]) => `${name}=${value}`)
    .join(',')})`;
}

describe('formatSeasonName', () => {
  it('spells out an episode-era code', () => {
    expect(formatSeasonName('e10a3', translate)).toBe('seasons.episode(episode=10,act=3)');
  });

  it('spells out a year-era code, whatever its case', () => {
    expect(formatSeasonName('V26A4', translate)).toBe('seasons.year(year=2026,act=4)');
  });

  it('returns an unrecognized code untouched', () => {
    expect(formatSeasonName('closed-beta', translate)).toBe('closed-beta');
  });
});

describe('splitSeasonName', () => {
  it('splits an episode-era code into its episode and act', () => {
    expect(splitSeasonName('e11a3', translate)).toEqual({
      era: 'seasons.episodeOnly(episode=11)',
      eraMark: 'seasons.episodeMark(episode=11)',
      act: 3,
    });
  });

  it('splits a year-era code into its year and act', () => {
    expect(splitSeasonName('v26a1', translate)).toEqual({ era: '2026', eraMark: '2026', act: 1 });
  });

  it('returns null for an unrecognized code', () => {
    expect(splitSeasonName('closed-beta', translate)).toBeNull();
  });
});
