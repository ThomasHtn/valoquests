import { ChallengeRung } from './challenges.model';
import { describeRung } from './challenges.utils';

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
