import { ChallengeIcon, ChallengeVisual } from './challenge-visual.model';
import { ChallengeDifficulty } from '../challenge.model';

/**
 * Icon per metric, keyed by the backend `ChallengeMetric` names.
 */
export const CHALLENGE_METRIC_ICONS: Readonly<Record<string, ChallengeIcon>> = {
  HEADSHOTS: 'skull',
  KILLS: 'crosshair',
  MATCHES_WON: 'trophy',
  ASSISTS: 'users',
  SCORE: 'star',
  DAMAGE_DEALT: 'swords',
  MATCHES_PLAYED: 'activity',
  ROUNDS_PLAYED: 'shield',
  KD: 'trending-up',
  PLAY_DAY: 'calendar',
  ACS: 'star',
  ADR: 'swords',
  HEADSHOT_RATE: 'skull',
};

/**
 * Fallback icon for unrecognized metrics.
 */
export const DEFAULT_CHALLENGE_ICON: ChallengeIcon = 'target';

/**
 * Tier treatment per difficulty, a heat ramp from green to red so the slots read as a ladder.
 * Keep each `tierColor` in sync with its class accent in `styles/colors.css`.
 */
export const CHALLENGE_DIFFICULTY_COLORS: Readonly<
  Record<ChallengeDifficulty, Omit<ChallengeVisual, 'icon'>>
> = {
  EASY: {
    tier: 'I',
    iconClass: 'text-accent-green',
    badgeClass: 'bg-accent-green/15',
    barClass: 'bg-accent-green',
    panelClass: 'border-accent-green/35 from-accent-green/12',
    tierColor: '#5fb88a',
  },
  NORMAL: {
    tier: 'II',
    iconClass: 'text-accent-blue',
    badgeClass: 'bg-accent-blue/15',
    barClass: 'bg-accent-blue',
    panelClass: 'border-accent-blue/35 from-accent-blue/12',
    tierColor: '#5a96be',
  },
  MEDIUM: {
    tier: 'III',
    iconClass: 'text-accent-gold',
    badgeClass: 'bg-accent-gold/15',
    barClass: 'bg-accent-gold',
    panelClass: 'border-accent-gold/35 from-accent-gold/12',
    tierColor: '#d9954a',
  },
  HARD: {
    tier: 'IV',
    iconClass: 'text-accent-pink',
    badgeClass: 'bg-accent-pink/15',
    barClass: 'bg-accent-pink',
    panelClass: 'border-accent-pink/35 from-accent-pink/12',
    tierColor: '#ec4899',
  },
  VERY_HARD: {
    tier: 'V',
    iconClass: 'text-accent-red',
    badgeClass: 'bg-accent-red/15',
    barClass: 'bg-accent-red',
    panelClass: 'border-accent-red/35 from-accent-red/12',
    tierColor: '#ff4655',
  },
};

/**
 * Daily treatment: cyan, the one accent the ladder does not use.
 */
export const DAILY_CHALLENGE_VISUAL: Omit<ChallengeVisual, 'icon'> = {
  tier: 'D',
  iconClass: 'text-accent-cyan',
  badgeClass: 'bg-accent-cyan/15',
  barClass: 'bg-accent-cyan',
  panelClass: 'border-accent-cyan/35 from-accent-cyan/12',
  tierColor: '#4ec9d6',
};
