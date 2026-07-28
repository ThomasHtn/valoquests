/**
 * Icon shown for a challenge, matched against a `@switch` in {@link ChallengeIconView} since each
 * Lucide icon is its own standalone directive.
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
 * Visual treatment applied to a challenge, shared by the weekly challenges card and the weekly
 * ranking table so both widgets render the same icon and color for a given challenge.
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
