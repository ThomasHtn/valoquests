import { ChallengeTier } from './challenge.model';

/**
 * The five tiers, easiest first; explicit because object key order is not a contract.
 */
export const CHALLENGE_DIFFICULTIES: readonly ChallengeTier[] = [
  'EASY',
  'NORMAL',
  'MEDIUM',
  'HARD',
  'VERY_HARD',
];
