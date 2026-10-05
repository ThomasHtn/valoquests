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
 * Colour modifier of the icons that carry one: food green, components cyan; the others stay brand.
 */
export const RULE_ICON_MODIFIERS: Partial<Record<RuleIcon, string>> = {
  food: 'rule-text__icon--food',
  components: 'rule-text__icon--components',
};

/**
 * Splits a rule on its inline tokens: `{icon}` and `*relief*`.
 */
export const RULE_TOKEN_PATTERN = /(\{[a-z]+\}|\*[^*]+\*)/;
