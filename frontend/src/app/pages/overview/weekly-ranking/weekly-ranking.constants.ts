/**
 * Border, background and text classes applied to a ranking row's position badge, from 1st place to
 * the neutral treatment used from 4th place onward.
 */
const PODIUM_BADGE_CLASSES: readonly string[] = [
  'border-accent-gold bg-accent-gold/20 text-accent-gold',
  'border-text-secondary bg-text-secondary/15 text-text-secondary',
  'border-accent-red bg-accent-red/20 text-accent-red',
];

/**
 * Default badge treatment for positions outside the podium.
 */
const DEFAULT_BADGE_CLASS = 'border-surface-600 bg-surface-700 text-text-secondary';

/**
 * Resolves the badge classes for a ranking row's position, highlighting the podium (1st to 3rd)
 * with a distinct color per rank.
 *
 * @param position - The player's 1-based ranking position.
 * @returns The Tailwind utility classes to apply to the position badge.
 */
export function resolvePositionBadgeClass(position: number): string {
  return PODIUM_BADGE_CLASSES[position - 1] ?? DEFAULT_BADGE_CLASS;
}
