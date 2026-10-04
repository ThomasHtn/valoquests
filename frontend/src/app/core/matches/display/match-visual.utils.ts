import { MatchResult } from '../match-result.model';
import { RESULT_ACCENT_CLASSES, RESULT_TEXT_CLASSES } from './match-visual.constants';

/**
 * Tailwind shadow utility accenting a match row's leading edge.
 */
export function resolveResultAccentClass(result: MatchResult): string {
  return RESULT_ACCENT_CLASSES[result];
}

/**
 * Tailwind text colour of the player's own score.
 */
export function resolveResultTextClass(result: MatchResult): string {
  return RESULT_TEXT_CLASSES[result];
}
