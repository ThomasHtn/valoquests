import { ChallengeDifficulty } from './challenge.model';
import { ChallengeIcon, ChallengeVisual } from './challenge-visual.model';

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
 * Resolves the icon and color treatment for a challenge.
 *
 * The icon reflects the challenge's metric (e.g. `"HEADSHOTS"` or `"KILLS + MATCHES_PLAYED"`,
 * matched on the first metric only), while the color reflects its difficulty tier so harder
 * challenges stand out. Shared by the weekly challenges card and the weekly ranking table so both
 * widgets read as one system.
 *
 * @param metric - The challenge's metric string.
 * @param difficulty - The challenge's difficulty tier.
 * @returns The visual treatment to apply for the challenge.
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

/**
 * Resolves the short category label shown for a challenge in place of its full name (e.g.
 * `"Kills"` rather than `"Élimination express"`), so the weekly challenges card and the weekly
 * ranking table both stay scannable at a glance.
 *
 * Composite challenges (e.g. `"KILLS + MATCHES_PLAYED"`) get every one of their metrics
 * translated and joined the same way the backend joins the raw metric string.
 *
 * @param metric - The challenge's metric string.
 * @param translate - Translation function resolving an `overview.weeklyChallenges.metric.*` key.
 * @returns The translated category label.
 */
export function resolveChallengeMetricLabel(
  metric: string,
  translate: (key: string) => string,
): string {
  return metric
    .split(' + ')
    .map((part) => translate(`overview.weeklyChallenges.metric.${part}`))
    .join(' + ');
}
