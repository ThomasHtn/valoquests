import { ChallengeDifficulty } from '../../../core/challenges/challenge.model';
import { ChallengeIcon, ChallengeVisual } from './weekly-challenges.model';

/**
 * Icon shown per challenge metric.
 *
 * Keyed by the backend's `ChallengeMetric` enum names. Composite challenges expose a
 * `" + "`-joined metric string; {@link resolveChallengeVisual} matches on the first metric only.
 */
const CHALLENGE_METRIC_ICONS: Readonly<Record<string, ChallengeIcon>> = {
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
};

/**
 * Fallback icon used for composite or unrecognized metrics.
 */
const DEFAULT_CHALLENGE_ICON: ChallengeIcon = 'target';

/**
 * Color treatment applied per challenge difficulty, from easiest to hardest.
 */
const CHALLENGE_DIFFICULTY_COLORS: Readonly<
  Record<ChallengeDifficulty, Omit<ChallengeVisual, 'icon'>>
> = {
  EASY: {
    iconClass: 'text-accent-green',
    badgeClass: 'bg-accent-green/15',
    barClass: 'bg-accent-green',
  },
  NORMAL: {
    iconClass: 'text-accent-blue',
    badgeClass: 'bg-accent-blue/15',
    barClass: 'bg-accent-blue',
  },
  MEDIUM: {
    iconClass: 'text-accent-gold',
    badgeClass: 'bg-accent-gold/15',
    barClass: 'bg-accent-gold',
  },
  HARD: {
    iconClass: 'text-accent-pink',
    badgeClass: 'bg-accent-pink/15',
    barClass: 'bg-accent-pink',
  },
  VERY_HARD: {
    iconClass: 'text-accent-red',
    badgeClass: 'bg-accent-red/15',
    barClass: 'bg-accent-red',
  },
};

/**
 * Resolves the icon and color treatment for a challenge row.
 *
 * The icon reflects the challenge's metric (e.g. `"HEADSHOTS"` or `"KILLS + MATCHES_PLAYED"`,
 * matched on the first metric only), while the color reflects its difficulty tier so harder
 * challenges stand out.
 *
 * @param metric - The challenge's metric string.
 * @param difficulty - The challenge's difficulty tier.
 * @returns The visual treatment to apply to the challenge row.
 */
export function resolveChallengeVisual(
  metric: string,
  difficulty: ChallengeDifficulty,
): ChallengeVisual {
  const [primaryMetric] = metric.split(' + ');
  return {
    icon: CHALLENGE_METRIC_ICONS[primaryMetric] ?? DEFAULT_CHALLENGE_ICON,
    ...CHALLENGE_DIFFICULTY_COLORS[difficulty],
  };
}
