import { RuleIcon } from './rule-text.model';

/**
 * Inline icon tokens (`{food}`); an unknown token renders as text so typos stay visible.
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
