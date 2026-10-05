import { describe, expect, it } from 'vitest';

import { countdownUnits } from './countdown.utils';

describe('countdownUnits', () => {
  // 2 days, 3 hours, 4 minutes, 5 seconds.
  const secondsLeft = 2 * 86_400 + 3 * 3_600 + 4 * 60 + 5;

  it('splits the time into days and padded clock slots', () => {
    expect(countdownUnits(secondsLeft, true)).toEqual([
      { value: '2', key: 'days' },
      { value: '03', key: 'hours' },
      { value: '04', key: 'minutes' },
      { value: '05', key: 'seconds' },
    ]);
  });

  it('folds the days into the hours without a days slot', () => {
    expect(countdownUnits(secondsLeft, false).map((unit) => unit.value)).toEqual([
      '51',
      '04',
      '05',
    ]);
  });
});
