import { MatchResult } from './match-result.model';

/**
 * Accent applied to the leading edge of a match row, echoing its result at a glance.
 *
 * Drawn as an inset shadow rather than a left border so it does not take part in layout: a border
 * would shift the first cell's content by its own width on every row that has one.
 *
 * Colour is the only visual channel carrying the result, so every call site must also expose it to
 * assistive technology (WCAG 1.4.1), which the match history does with a visually hidden label.
 */
const RESULT_ACCENT_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'shadow-[inset_3px_0_0_var(--color-accent-green)]',
  LOSS: 'shadow-[inset_3px_0_0_var(--color-accent-red)]',
  DRAW: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
  REMAKE: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
  UNKNOWN: 'shadow-[inset_3px_0_0_var(--color-surface-600)]',
};

/**
 * Text colour applied to the player's own score, echoing the match result a second time.
 *
 * A round score says nothing on its own about which side the reader was on; colouring the ally
 * side answers that without adding a column. Undecided results stay neutral rather than picking a
 * side.
 */
const RESULT_TEXT_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'text-accent-green',
  LOSS: 'text-accent-red',
  DRAW: 'text-text-primary',
  REMAKE: 'text-text-primary',
  UNKNOWN: 'text-text-primary',
};

/**
 * Resolves the leading-edge accent for a match row.
 *
 * @param result - The match's result.
 * @returns The Tailwind shadow utility to apply.
 */
export function resolveResultAccentClass(result: MatchResult): string {
  return RESULT_ACCENT_CLASSES[result];
}

/**
 * Resolves the text colour carrying a match's result on the player's own score.
 *
 * @param result - The match's result.
 * @returns The Tailwind text-colour utility to apply.
 */
export function resolveResultTextClass(result: MatchResult): string {
  return RESULT_TEXT_CLASSES[result];
}
