import { describe, expect, it } from 'vitest';

import { resolveDamageHintKey } from './match-format.utils';

describe('resolveDamageHintKey', () => {
  it('explains a match that never entered the ladder', () => {
    expect(resolveDamageHintKey(0)).toBe('playerProfile.matches.damage.unvalued');
  });

  it('explains a match that kept its full value', () => {
    expect(resolveDamageHintKey(100)).toBe('playerProfile.matches.damage.full');
  });

  it('explains a match reduced by the ladder, the case drawn with a down arrow', () => {
    expect(resolveDamageHintKey(50)).toBe('playerProfile.matches.damage.reduced');
    expect(resolveDamageHintKey(25)).toBe('playerProfile.matches.damage.reduced');
  });
});
