import { describe, expect, it } from 'vitest';

import { resolveCurrentSeasonId, resolveYieldToneClass } from './player-profile.utils';

describe('resolveCurrentSeasonId', () => {
  it('prefers the active season', () => {
    expect(
      resolveCurrentSeasonId([
        { id: 2, name: 'e9a2', active: false },
        { id: 1, name: 'e9a1', active: true },
      ]),
    ).toBe(1);
  });

  it('falls back to the first known season, then to null', () => {
    expect(resolveCurrentSeasonId([{ id: 5, name: 'e9a5', active: false }])).toBe(5);
    expect(resolveCurrentSeasonId([])).toBeNull();
  });
});

describe('resolveYieldToneClass', () => {
  it('is amber at full value and muted once the ladder takes a cut', () => {
    expect(resolveYieldToneClass(100)).toBe('text-brand-500');
    expect(resolveYieldToneClass(50)).toBe('text-text-secondary');
  });
});
