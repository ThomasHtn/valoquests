import { describe, expect, it } from 'vitest';

import { parseRuleText } from './rule-text.utils';

describe('parseRuleText', () => {
  it('splits plain words, relief and icons', () => {
    expect(parseRuleText('Earn {food} *food* daily')).toEqual([
      { text: 'Earn ', strong: false, icon: null, modifier: '' },
      { text: '', strong: false, icon: 'food', modifier: 'rule-text__icon--food' },
      { text: ' ', strong: false, icon: null, modifier: '' },
      { text: 'food', strong: true, icon: null, modifier: '' },
      { text: ' daily', strong: false, icon: null, modifier: '' },
    ]);
  });

  it('leaves the brand default to icons without a colour of their own', () => {
    expect(parseRuleText('{rocket}')).toEqual([
      { text: '', strong: false, icon: 'rocket', modifier: '' },
    ]);
  });

  it('keeps an unknown token as text', () => {
    expect(parseRuleText('{typo}')).toEqual([
      { text: '{typo}', strong: false, icon: null, modifier: '' },
    ]);
  });
});
