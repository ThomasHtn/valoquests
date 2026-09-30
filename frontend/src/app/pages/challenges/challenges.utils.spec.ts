import { ChallengeProgress } from '@core/challenges/challenge.model';
import {
  BoardMark,
  BoardRow,
  ChallengeOperator,
  ChallengeRung,
  MarkDetail,
} from './challenges.model';
import {
  describeRung,
  orderOperators,
  splitCompactFigure,
  toBoardMark,
  toMarkDetail,
  toShelves,
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
    expect(detail).toMatchObject({ gapLabel: '+100', fill: 50, over: 50, targetAt: 50 });
  });

  it('measures what remains while under way', () => {
    const detail = toMarkDetail(rung(40, false), 100, 'red', 'en-US');
    expect(detail).toMatchObject({ state: 'open', gap: 'remaining', gapLabel: '60', gapCount: 60 });
    expect(detail).toMatchObject({ fill: 40, over: 0, targetAt: 100 });
  });

  it('shows no gap once validated exactly on target', () => {
    expect(toMarkDetail(rung(100, true), 100, 'red', 'en-US')).toMatchObject({
      gap: 'none',
      gapLabel: '',
      fill: 100,
    });
  });

  it('drops the target and gauge on an open-ended challenge', () => {
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

describe('toShelves', () => {
  // Only what the shelves read; the rest of a row does not matter here.
  const row = (key: string, daily: boolean, done: readonly boolean[]) =>
    ({
      key,
      daily,
      tone: `tone-${key}`,
      marks: done.map((d) => ({ done: d }) as BoardMark),
    }) as unknown as BoardRow;

  it('gives each operator one slot per weekly challenge, the day left out', () => {
    const shelves = toShelves(
      [
        row('d', true, [true, true]),
        row('w1', false, [true, false]),
        row('w2', false, [false, true]),
      ],
      2,
    );
    expect(shelves).toEqual([
      [
        { key: 'w1', tone: 'tone-w1', done: true },
        { key: 'w2', tone: 'tone-w2', done: false },
      ],
      [
        { key: 'w1', tone: 'tone-w1', done: false },
        { key: 'w2', tone: 'tone-w2', done: true },
      ],
    ]);
  });
});
