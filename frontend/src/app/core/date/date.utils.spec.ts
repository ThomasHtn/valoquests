import { describe, expect, it } from 'vitest';

import { addDays, daysBetween, localMidnight, parseIsoDate } from './date.utils';

describe('date utils', () => {
  it('parses a YYYY-MM-DD date into numbers', () => {
    expect(parseIsoDate('2026-03-09')).toEqual({ year: 2026, month: 3, day: 9 });
  });

  it('shifts across month and year ends', () => {
    expect(addDays('2026-12-28', 6)).toBe('2027-01-03');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('counts whole days across a daylight-saving change', () => {
    expect(daysBetween('2026-10-19', '2026-10-26')).toBe(7);
    expect(daysBetween('2026-10-26', '2026-10-19')).toBe(-7);
  });

  it('builds local midnight, shifted when asked', () => {
    const midnight = localMidnight('2026-10-05', 1);
    expect([midnight.getFullYear(), midnight.getMonth(), midnight.getDate()]).toEqual([2026, 9, 6]);
    expect(midnight.getHours()).toBe(0);
  });
});
