import { describe, expect, it } from 'vitest';

import { buildTourDailyRow, buildTourWeek, endOfDay, startOfWeek } from './tour.utils';

const translate = (key: string, params?: Record<string, string | number>): string =>
  params ? `${key}:${JSON.stringify(params)}` : key;

const operators = [
  { playerId: 1, name: 'Kairo', portrait: null },
  { playerId: 2, name: 'Sable', portrait: null },
];

describe('startOfWeek', () => {
  it('goes back to Monday midnight', () => {
    const monday = startOfWeek(new Date(2026, 9, 1, 14, 33).getTime());
    expect(monday.getDay()).toBe(1);
    expect(monday.getDate()).toBe(28);
    expect(monday.getHours()).toBe(0);
  });

  it('keeps a Sunday in the week it closes', () => {
    expect(startOfWeek(new Date(2026, 9, 4, 22).getTime()).getDate()).toBe(28);
  });
});

describe('buildTourWeek', () => {
  const week = buildTourWeek([4, 2], 1, 4, new Date(2026, 8, 28), 'fr-FR', translate);

  it('closes the tallied days, runs today and leaves the rest ahead', () => {
    expect(week.map((day) => day.state)).toEqual([
      'closed',
      'closed',
      'now',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
    ]);
  });

  it('counts each day and draws none ahead', () => {
    expect(week.map((day) => day.doneCount)).toEqual([4, 2, 1, 0, 0, 0, 0]);
    expect(week.map((day) => day.drawn)).toEqual([true, true, true, false, false, false, false]);
  });

  it('labels the days from Monday', () => {
    expect(week[0].weekday).toBe('Lun');
    expect(week[0].date).toBe('28');
  });
});

describe('buildTourDailyRow', () => {
  const row = buildTourDailyRow(
    { key: 'session', target: 3, survivors: 6, progress: [3, 1] },
    operators,
    0,
    'fr-FR',
    translate,
  );

  it('lays the sample out as a running daily row', () => {
    expect(row.daily).toBe(true);
    expect(row.closesAt).toBe(0);
    expect(row.mark).toBe('D');
    expect(row.survivors).toBe(6);
  });

  it('marks who validated it, one segment per match', () => {
    expect(row.marks.map((mark) => mark.done)).toEqual([true, false]);
    expect(row.marks[1].segments).toEqual([true, false, false]);
  });
});

describe('endOfDay', () => {
  it('lands on the next local midnight', () => {
    expect(endOfDay(new Date(2026, 9, 1, 14, 33).getTime())).toBe(new Date(2026, 9, 2).getTime());
  });
});
