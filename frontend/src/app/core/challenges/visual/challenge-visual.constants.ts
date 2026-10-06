import { ChallengeVisual } from './challenge-visual.model';
import { ChallengeTier } from '../challenge.model';

/**
 * Tier treatment per difficulty, a heat ramp from green to red so the slots read as a ladder.
 */
export const CHALLENGE_DIFFICULTY_COLORS: Readonly<Record<ChallengeTier, ChallengeVisual>> = {
  EASY: {
    tier: 'I',
    tierColor: 'var(--color-accent-green)',
  },
  NORMAL: {
    tier: 'II',
    tierColor: 'var(--color-accent-blue)',
  },
  MEDIUM: {
    tier: 'III',
    tierColor: 'var(--color-accent-gold)',
  },
  HARD: {
    tier: 'IV',
    tierColor: 'var(--color-accent-pink)',
  },
  VERY_HARD: {
    tier: 'V',
    tierColor: 'var(--color-accent-red)',
  },
};

/**
 * Daily treatment: cyan, the one accent the ladder does not use.
 */
export const DAILY_CHALLENGE_VISUAL: ChallengeVisual = {
  tier: 'D',
  tierColor: 'var(--color-accent-cyan)',
};
