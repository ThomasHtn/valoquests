import { describe, expect, it } from 'vitest';

import { buildSeasonPickerOptions } from './season-picker.utils';

/**
 * Fake translation echoing the key and its parameters.
 */
function translate(key: string, params?: Readonly<Record<string, string | number>>): string {
  const suffix = Object.entries(params ?? {})
    .map(([name, value]) => `${name}=${value}`)
    .join(',');
  return suffix ? `${key}(${suffix})` : key;
}

describe('buildSeasonPickerOptions', () => {
  it('badges the episode and labels the act', () => {
    expect(buildSeasonPickerOptions([{ id: 3, name: 'e11a5', active: true }], translate)).toEqual([
      {
        id: 3,
        mark: 'seasons.episodeMark(episode=11)',
        label: 'seasons.actOnly(act=5)',
        fullName: 'seasons.episode(episode=11,act=5)',
        era: 'seasons.episodeOnly(episode=11)',
      },
    ]);
  });

  it('falls back to the raw code without a badge', () => {
    expect(
      buildSeasonPickerOptions([{ id: 9, name: 'beta', active: false }], translate)[0],
    ).toMatchObject({ mark: null, label: 'beta' });
  });
});
