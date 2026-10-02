import { describe, expect, it } from 'vitest';

import { formatElapsed } from './relative-time.utils';

const at = Date.parse('2026-10-02T12:00:00Z');

describe('formatElapsed', () => {
  it('counts minutes, then hours, then days', () => {
    expect(formatElapsed('2026-10-02T11:48:00Z', at, 'en')).toBe('12 min. ago');
    expect(formatElapsed('2026-10-02T09:00:00Z', at, 'en')).toBe('3 hr. ago');
    expect(formatElapsed('2026-09-30T12:00:00Z', at, 'en')).toBe('2 days ago');
  });

  it('reads as now under a minute', () => {
    expect(formatElapsed('2026-10-02T11:59:30Z', at, 'en')).toBe('now');
  });
});
