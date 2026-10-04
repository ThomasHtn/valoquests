import { MatchResult } from '../match-result.model';

/**
 * Result accent on a match row's leading edge, an inset shadow so it never shifts layout.
 * Colour-only: call sites must also expose the result to assistive technology (WCAG 1.4.1).
 */
export const RESULT_ACCENT_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'shadow-[inset_3px_0_0_var(--color-accent-green)]',
  LOSS: 'shadow-[inset_3px_0_0_var(--color-accent-red)]',
  DRAW: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
  REMAKE: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
  UNKNOWN: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
};

/**
 * Colour of the player's own score, telling which side was theirs; undecided stays neutral.
 */
export const RESULT_TEXT_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'text-accent-green',
  LOSS: 'text-accent-red',
  DRAW: 'text-text-primary',
  REMAKE: 'text-text-primary',
  UNKNOWN: 'text-text-primary',
};
