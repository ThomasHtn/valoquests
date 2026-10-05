import { ChallengeProgress } from '@core/challenges/challenge.model';
import { ChallengeOperator } from '@core/challenges/card/challenge-card.model';
import {
  formatDayMonth,
  formatDayOfMonth,
  formatWeekday,
  orderOperators,
  toRuleParts,
} from './challenges.utils';

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

describe('date labels', () => {
  it('capitalises a short weekday and drops its dot', () => {
    expect(formatWeekday('2026-10-05', 'fr-FR', 'short')).toBe('Lun');
  });

  it('keeps a long weekday as the locale writes it', () => {
    expect(formatWeekday('2026-10-05', 'fr-FR', 'long')).toBe('lundi');
  });

  it('spells the month and isolates the day', () => {
    expect(formatDayMonth('2026-10-05', 'fr-FR')).toBe('5 octobre');
    expect(formatDayOfMonth('2026-10-05', 'fr-FR')).toBe('5');
  });
});
