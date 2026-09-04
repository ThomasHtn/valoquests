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
  ACS: 'star',
  ADR: 'swords',
  HEADSHOT_RATE: 'skull',
};

/**
 * Fallback icon used for composite or unrecognized metrics.
 */
const DEFAULT_CHALLENGE_ICON: ChallengeIcon = 'target';

/**
 * Tier rank and color treatment applied per challenge difficulty, from easiest to hardest.
 *
 * The scale is a heat ramp — green, blue, amber, pink, red — so the five weekly slots read as an
 * escalating ladder rather than five unrelated categories. Each tier carries its accent twice: as
 * the Tailwind classes most callers apply, and as the bare hex a component stylesheet lights a
 * whole block from through one custom property. Both must move together — keep the hexes in sync
 * with `styles/colors.css`. The hardest tier takes `accent-red`, the same hue as damage and
 * guardian health: the reward is what a very hard challenge is *for*.
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
 * Treatment of the daily challenge, which has no difficulty: cyan, the one accent the ladder
 * above does not use, so a day's challenge is never mistaken for a weekly slot.
 */
const DAILY_CHALLENGE_VISUAL: Omit<ChallengeVisual, 'icon'> = {
  tier: 'D',
  iconClass: 'text-accent-cyan',
  badgeClass: 'bg-accent-cyan/15',
  barClass: 'bg-accent-cyan',
  panelClass: 'border-accent-cyan/35 from-accent-cyan/12',
  tierColor: '#4ec9d6',
};

/**
 * Resolves the tier rank and color treatment of a difficulty, without an icon.
 *
 * Used where a difficulty is shown on its own rather than through a challenge — the rules page's
 * reward ladder — so the tier reads with the same color there as on the weekly board.
 *
 * @param difficulty - The difficulty tier, or `null` for the daily challenge.
 * @returns The visual treatment to apply for the tier.
 */
export function resolveDifficultyVisual(
  difficulty: ChallengeDifficulty | null,
): Omit<ChallengeVisual, 'icon'> {
  return difficulty === null ? DAILY_CHALLENGE_VISUAL : CHALLENGE_DIFFICULTY_COLORS[difficulty];
}

/**
 * Resolves the icon and color treatment for a challenge.
 *
 * The icon reflects the challenge's metric (e.g. `"HEADSHOTS"` or `"KILLS + MATCHES_PLAYED"`,
 * matched on the first metric only), while the color reflects its difficulty tier so harder
 * challenges stand out. Shared by every screen drawing a challenge so they read as one system.
 *
 * @param metric - The challenge's metric string.
 * @param difficulty - The challenge's difficulty tier, or `null` for the daily challenge.
 * @returns The visual treatment to apply for the challenge.
 */
export function resolveChallengeVisual(
  metric: string,
  difficulty: ChallengeDifficulty | null,
): ChallengeVisual {
  const [primaryMetric] = metric.split(' + ');
  return {
    icon: CHALLENGE_METRIC_ICONS[primaryMetric] ?? DEFAULT_CHALLENGE_ICON,
    ...resolveDifficultyVisual(difficulty),
  };
}

/**
 * Resolves the short category label shown for a challenge in place of its full name (e.g.
 * `"Kills"` rather than `"Élimination express"`), so a board stays scannable at a glance.
 *
 * Composite challenges (e.g. `"KILLS + MATCHES_PLAYED"`) get every one of their metrics
 * translated and joined the same way the backend joins the raw metric string.
 *
 * @param metric - The challenge's metric string.
 * @param translate - Translation function resolving a `common.metric.*` key.
 * @returns The translated category label.
 */
export function resolveChallengeMetricLabel(
  metric: string,
  translate: (key: string) => string,
): string {
  return metric
    .split(' + ')
    .map((part) => translate(`common.metric.${part}`))
    .join(' + ');
}
