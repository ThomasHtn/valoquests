import { RULE_ICONS } from './rule-text.constants';

/**
 * One of the icon tokens a rule may name inline.
 */
export type RuleIcon = (typeof RULE_ICONS)[number];

/**
 * One run of a rule's text: plain words, a word set in relief, or an icon standing for a word.
 */
export interface RuleRun {
  /**
   * Words of the run, empty for an icon.
   */
  readonly text: string;

  /**
   * Whether the run is set in relief.
   */
  readonly strong: boolean;

  /**
   * Icon standing for the word, or `null` for plain text.
   */
  readonly icon: RuleIcon | null;

  /**
   * Icon colour modifier, empty for the brand default.
   */
  readonly modifier: string;
}
