/**
 * Icon shown for a challenge, matched against a `@switch` in the template since each Lucide icon
 * is its own standalone directive.
 */
export type ChallengeIcon =
  | 'skull'
  | 'crosshair'
  | 'trophy'
  | 'users'
  | 'star'
  | 'swords'
  | 'activity'
  | 'shield'
  | 'trending-up'
  | 'calendar'
  | 'target';

/**
 * Visual treatment applied to a challenge row: its icon and the Tailwind utility classes used for
 * the icon, its badge background and the progress bar fill.
 *
 * Classes are pre-built as full literal strings (rather than composed from an accent name at
 * render time) so Tailwind's build-time class scanner can find them in this file.
 */
export interface ChallengeVisual {
  readonly icon: ChallengeIcon;
  readonly iconClass: string;
  readonly badgeClass: string;
  readonly barClass: string;
}

/**
 * Single row of the weekly challenges list: a challenge paired with its resolved visual treatment.
 */
export interface ChallengeRow {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly completedPlayers: number;
  readonly totalPlayers: number;
  readonly completionPercentage: number;
  readonly points: number;
  readonly visual: ChallengeVisual;
}
