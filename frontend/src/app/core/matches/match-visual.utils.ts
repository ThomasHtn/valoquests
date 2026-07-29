import { MatchResult } from './match-result.model';

/**
 * Left border color applied to a match row, echoing its result at a glance.
 */
const RESULT_BORDER_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'border-accent-green',
  LOSS: 'border-accent-red',
  DRAW: 'border-surface-600',
  REMAKE: 'border-surface-600',
  UNKNOWN: 'border-surface-600',
};

/**
 * Text color applied to a match row's result label, paired with {@link resolveResultBorderClass}.
 */
const RESULT_TEXT_CLASSES: Readonly<Record<MatchResult, string>> = {
  WIN: 'text-accent-green',
  LOSS: 'text-accent-red',
  DRAW: 'text-text-secondary',
  REMAKE: 'text-text-secondary',
  UNKNOWN: 'text-text-secondary',
};

/**
 * Resolves the left border color for a match row.
 *
 * @param result - The match's result.
 * @returns The Tailwind border color utility to apply.
 */
export function resolveResultBorderClass(result: MatchResult): string {
  return RESULT_BORDER_CLASSES[result];
}

/**
 * Resolves the text color for a match row's result label.
 *
 * @param result - The match's result.
 * @returns The Tailwind text color utility to apply.
 */
export function resolveResultTextClass(result: MatchResult): string {
  return RESULT_TEXT_CLASSES[result];
}
