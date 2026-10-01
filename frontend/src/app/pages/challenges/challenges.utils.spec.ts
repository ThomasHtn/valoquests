import { ChallengeProgress } from '@core/challenges/challenge.model';
import { ChallengeOperator, ChallengeRung, MarkDetail } from './challenges.model';
import {
  describeRung,
  orderOperators,
  splitCompactFigure,
  toBoardMark,
  toMarkDetail,
  toRuleParts,
} from './challenges.utils';

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

// Echoes the key and its parameters so assertions read the raw wording inputs.
const echo = (key: string, params: Record<string, string | number>) =>
  `${key} ${JSON.stringify(params)}`;

describe('describeRung', () => {
  it('spells out exact figures and what remains', () => {
    expect(describeRung(rung(2999, false), 3000, 'en-US', echo)).toBe(
      'open {"value":"2,999","target":"3,000","remaining":"1","count":1}',
    );
  });

  it('drops what remains once the operator validated it', () => {
    expect(describeRung(rung(3200, true), 3000, 'en-US', echo)).toBe(
      'done {"value":"3,200","target":"3,000"}',
    );
  });

  it('words an open-ended challenge by its value alone', () => {
    expect(describeRung(rung(12, false), null, 'en-US', echo)).toBe('openEnded {"value":"12"}');
    expect(describeRung(rung(12, true), null, 'en-US', echo)).toBe('openEndedDone {"value":"12"}');
  });
});

describe('toMarkDetail', () => {
  it('keeps the value uncapped and measures the surplus past the target', () => {
    const detail = toMarkDetail(rung(200, true), 100, 'red', 'en-US');
    expect(detail).toMatchObject({ state: 'done', value: '200', target: '100', gap: 'surplus' });
    expect(detail).toMatchObject({ gapLabel: '+100', gapCount: 100 });
  });

  it('measures what remains while under way', () => {
    const detail = toMarkDetail(rung(40, false), 100, 'red', 'en-US');
    expect(detail).toMatchObject({ state: 'open', gap: 'remaining', gapLabel: '60', gapCount: 60 });
  });

  it('shows no gap once validated exactly on target', () => {
    expect(toMarkDetail(rung(100, true), 100, 'red', 'en-US')).toMatchObject({
      gap: 'none',
      gapLabel: '',
    });
  });

  it('drops the target on an open-ended challenge', () => {
    expect(toMarkDetail(rung(0, false), null, 'red', 'en-US')).toMatchObject({
      state: 'idle',
      target: '',
      gap: 'none',
    });
  });
});

const detail = {} as MarkDetail;

const operator = (playerId: number): ChallengeOperator => ({
  playerId,
  name: `P${playerId}`,
  portrait: null,
});

const weekly = (targetValue: number, lines: [number, number, boolean][]): ChallengeProgress =>
  ({
    targetValue,
    players: lines.map(([playerId, currentValue, completed]) => ({
      playerId,
      currentValue,
      completed,
    })),
  }) as unknown as ChallengeProgress;

describe('orderOperators', () => {
  const roster = [operator(1), operator(2), operator(3)];
  const week = [
    weekly(4, [
      [1, 1, false],
      [2, 4, true],
      [3, 3, false],
    ]),
  ];

  it('ranks by validated, then progress, then roster order', () => {
    expect(orderOperators(roster, week, null).map((entry) => entry.playerId)).toEqual([2, 3, 1]);
  });

  it('puts the pinned operator first', () => {
    expect(orderOperators(roster, week, 1).map((entry) => entry.playerId)).toEqual([1, 2, 3]);
  });
});

describe('splitCompactFigure', () => {
  it('splits the compact unit from the figure', () => {
    expect(splitCompactFigure('12,7k')).toEqual({ figure: '12,7', unit: 'k' });
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

describe('toRuleParts', () => {
  it('sets apart each number', () => {
    expect(toRuleParts('Terminer 3 parties avec au moins 8 headshots.')).toEqual([
      { text: 'Terminer ', number: false },
      { text: '3', number: true },
      { text: ' parties avec au moins ', number: false },
      { text: '8', number: true },
      { text: ' headshots.', number: false },
    ]);
  });

  it('keeps a spaced thousand whole', () => {
    expect(toRuleParts('Cumuler 25\u202f000 dégâts.')).toEqual([
      { text: 'Cumuler ', number: false },
      { text: '25\u202f000', number: true },
      { text: ' dégâts.', number: false },
    ]);
  });

  it('returns a rule without numbers as one plain stretch', () => {
    expect(toRuleParts('Gagner une partie.')).toEqual([
      { text: 'Gagner une partie.', number: false },
    ]);
  });
});
