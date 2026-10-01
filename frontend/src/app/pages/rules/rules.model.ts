/**
 * Icon a constant of the closing sheet is drawn with, the same vocabulary as the rest of the
 * gameplay pages: a wheat ear is food, a wrench is components.
 */
export type RuleConstantIcon = 'sync' | 'base' | 'food' | 'components' | 'bed';

/**
 * One value of the closing sheet: its dictionary key and how its label is marked.
 */
export interface RuleConstant {
  /**
   * Key under `rules.sections.constants.items`, holding the label and the value.
   */
  readonly key: string;

  /**
   * Icon set before the label.
   */
  readonly icon: RuleConstantIcon;

  /**
   * Text colour class of the icon.
   */
  readonly tone: string;
}
