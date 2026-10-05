import { MatchResult } from '../match-result.model';
import { RESULT_TONES } from './match-visual.constants';
import { ResultTone } from './match-visual.model';

/**
 * Tone of a match result.
 */
export function resolveResultTone(result: MatchResult): ResultTone {
  return RESULT_TONES[result];
}
