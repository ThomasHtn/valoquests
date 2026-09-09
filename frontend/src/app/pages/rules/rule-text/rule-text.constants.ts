import { RuleIcon } from './rule-text.model';

/**
 * Icons a rule may name inline, by the token written in the dictionary (`{food}`, `{guardian}`).
 *
 * The same vocabulary as the rest of the gameplay pages: a wheat ear is food everywhere, a skull is
 * the guardian everywhere. A token outside this list is rendered as its own text, so a typo in a
 * dictionary shows up on screen rather than vanishing.
 */
export const RULE_ICONS = [
  'food',
  'components',
  'damage',
  'guardian',
  'wounded',
  'base',
  'challenge',
  'streak',
  'points',
  'rocket',
  'day',
] as const;

/**
 * Colour of the icons that carry one: food green, components cyan.
 */
export const ICON_TONES: Partial<Record<RuleIcon, string>> = {
  food: 'text-accent-green',
  components: 'text-accent-cyan',
};

/**
 * Splits a rule on its inline tokens: `{icon}` and `*relief*`.
 */
export const TOKEN = /(\{[a-z]+\}|\*[^*]+\*)/;
