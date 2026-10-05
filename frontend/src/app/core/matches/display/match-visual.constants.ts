import { MatchResult } from '../match-result.model';
import { ResultTone } from './match-visual.model';

/**
 * Tone of each result, telling which side was the player's; undecided stays neutral.
 * Colour-only: call sites must also expose the result to assistive technology (WCAG 1.4.1).
 */
export const RESULT_TONES: Readonly<Record<MatchResult, ResultTone>> = {
  WIN: 'win',
  LOSS: 'loss',
  DRAW: 'neutral',
  REMAKE: 'neutral',
  UNKNOWN: 'neutral',
};
