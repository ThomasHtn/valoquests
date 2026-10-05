import { TranslateFn } from '@core/i18n/translation.model';

import { ChallengeRung, MarkDetail } from './challenge-card.model';
import {
  describeRung,
  splitCompactFigure,
  toBoardMark,
  toMarkDetail,
} from './challenge-card.utils';

const rung = (value: number, done: boolean): ChallengeRung => ({
  playerId: 1,
  name: 'Neon',
  portrait: null,
  fraction: 0,
  value,
  valueLabel: '',
  targetLabel: '',
  done,
  idle: value === 0,
});

// Echoes key and params so assertions read the raw wording inputs.
const echo: TranslateFn = (key, params) => `${key} ${JSON.stringify(params)}`;

describe('describeRung', () => {
  it('spells out exact figures and what remains', () => {
    expect(describeRung(rung(2999, false), 3000, 'en', echo)).toBe(
      'open {"value":"2,999","target":"3,000","remaining":"1","count":1}',
    );
  });

  it('drops what remains once the operator validated it', () => {
    expect(describeRung(rung(3200, true), 3000, 'en', echo)).toBe(
      'done {"value":"3,200","target":"3,000"}',
    );
  });

  it('words an open-ended challenge by its value alone', () => {
    expect(describeRung(rung(12, false), null, 'en', echo)).toBe('openEnded {"value":"12"}');
    expect(describeRung(rung(12, true), null, 'en', echo)).toBe('openEndedDone {"value":"12"}');
  });
});

describe('toMarkDetail', () => {
  it('keeps the value uncapped and measures the surplus past the target', () => {
    const detail = toMarkDetail(rung(200, true), 100, 'red', 'en');
    expect(detail).toMatchObject({ state: 'done', value: '200', target: '100', gap: 'surplus' });
    expect(detail).toMatchObject({ gapLabel: '+100', gapCount: 100 });
  });

  it('measures what remains while under way', () => {
    const detail = toMarkDetail(rung(40, false), 100, 'red', 'en');
    expect(detail).toMatchObject({ state: 'open', gap: 'remaining', gapLabel: '60', gapCount: 60 });
  });

  it('shows no gap once validated exactly on target', () => {
    expect(toMarkDetail(rung(100, true), 100, 'red', 'en')).toMatchObject({
      gap: 'none',
      gapLabel: '',
    });
  });

  it('drops the target on an open-ended challenge', () => {
    expect(toMarkDetail(rung(0, false), null, 'red', 'en')).toMatchObject({
      state: 'idle',
      target: '',
      gap: 'none',
    });
  });
});

const detail = {} as MarkDetail;

describe('splitCompactFigure', () => {
  it('splits the compact unit from the figure', () => {
    expect(splitCompactFigure('12.7k')).toEqual({ figure: '12.7', unit: 'k' });
    expect(splitCompactFigure('450')).toEqual({ figure: '450', unit: '' });
  });
});

describe('toBoardMark', () => {
  it('lights one segment per unit reached on a small target', () => {
    expect(toBoardMark(rung(2, false), 4, '', detail).segments).toEqual([true, true, false, false]);
    expect(toBoardMark(rung(1, true), 3, '', detail).segments).toEqual([true, true, true]);
  });

  it('runs a continuous line past twelve units', () => {
    expect(toBoardMark(rung(2, false), 25_000, '', detail).segments).toEqual([]);
  });
});
