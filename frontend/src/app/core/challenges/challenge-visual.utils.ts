import { ChallengeDifficulty } from './challenge.model';
import { ChallengeIcon, ChallengeVisual } from './challenge-visual.model';

/**
 * Icon shown per challenge metric.
 *
 * Keyed by the backend's `ChallengeMetric` enum names. Composite challenges expose a
 * `" + "`-joined metric string; {@link resolveChallengeVisual} matches on the first metric only.
 *
 * The `*_PROGRESS` keys are the same metrics asked as an improvement on the player's own recent form
 * rather than as an absolute threshold. The backend suffixes them so the two can be told apart: their
 * target is a percentage gain, not a value to reach. They all share the rising-trend icon.
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
  ACS: 'star',
  ADR: 'swords',
  HEADSHOT_RATE: 'skull',
  KD_PROGRESS: 'trending-up',
  ACS_PROGRESS: 'trending-up',
  ADR_PROGRESS: 'trending-up',
  HEADSHOT_RATE_PROGRESS: 'trending-up',
};

/**
 * Fallback icon used for composite or unrecognized metrics.
 */
const DEFAULT_CHALLENGE_ICON: ChallengeIcon = 'target';

/**
 * Tier rank and color treatment applied per challenge difficulty, from easiest to hardest.
 *
 * The scale is a heat ramp — green, blue, amber, pink, red — so the five weekly slots read as an
 * escalating ladder rather than five unrelated categories. The hardest tier takes `accent-red`,
 * the same hue as damage and boss health: the reward is what a very hard challenge is *for*, and
 * nothing else on these screens is red enough to be confused with it.
 */
const CHALLENGE_DIFFICULTY_COLORS: Readonly<
  Record<ChallengeDifficulty, Omit<ChallengeVisual, 'icon'>>
> = {
  EASY: {
    tier: 'I',
    iconClass: 'text-accent-green',
    badgeClass: 'bg-accent-green/15',
    barClass: 'bg-accent-green',
    panelClass: 'border-accent-green/35 from-accent-green/12',
  },
  NORMAL: {
    tier: 'II',
    iconClass: 'text-accent-blue',
    badgeClass: 'bg-accent-blue/15',
    barClass: 'bg-accent-blue',
    panelClass: 'border-accent-blue/35 from-accent-blue/12',
  },
  MEDIUM: {
    tier: 'III',
    iconClass: 'text-accent-gold',
    badgeClass: 'bg-accent-gold/15',
    barClass: 'bg-accent-gold',
    panelClass: 'border-accent-gold/35 from-accent-gold/12',
  },
  HARD: {
    tier: 'IV',
    iconClass: 'text-accent-pink',
    badgeClass: 'bg-accent-pink/15',
    barClass: 'bg-accent-pink',
    panelClass: 'border-accent-pink/35 from-accent-pink/12',
  },
  VERY_HARD: {
    tier: 'V',
    iconClass: 'text-accent-red',
    badgeClass: 'bg-accent-red/15',
    barClass: 'bg-accent-red',
    panelClass: 'border-accent-red/35 from-accent-red/12',
  },
};

/**
 * Resolves the tier rank and color treatment of a difficulty, without an icon.
 *
 * Used where a difficulty is shown on its own rather than through a challenge — the rules page's
 * damage ladder — so the tier reads with the same color there as on the weekly board.
 *
 * @param difficulty - The difficulty tier.
 * @returns The visual treatment to apply for the tier.
 */
export function resolveDifficultyVisual(
  difficulty: ChallengeDifficulty,
): Omit<ChallengeVisual, 'icon'> {
  return CHALLENGE_DIFFICULTY_COLORS[difficulty];
}

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
    ...resolveDifficultyVisual(difficulty),
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
