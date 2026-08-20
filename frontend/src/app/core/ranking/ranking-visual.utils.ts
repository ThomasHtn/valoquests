/**
 * Text color applied to a ranking row's position badge. Only 1st place gets a distinct (gold)
 * treatment; every other position, podium or not, uses the same neutral color.
 */
const PODIUM_TEXT_CLASSES: readonly string[] = ['text-accent-gold'];

/**
 * Default badge treatment for positions outside the podium.
 */
const DEFAULT_TEXT_CLASS = 'text-text-secondary';

/**
 * Resolves the text color for a ranking row's position badge, highlighting only 1st place with a
 * distinct color. `null` (an inactive player, who never consumes a ranking slot) gets the same
 * neutral treatment as any other non-1st position.
 *
 * Shared by the position badge used on the podium, the current-week ranking and the ranking
 * history page so all three read as one system.
 *
 * @param position - The player's 1-based ranking position, or `null` when inactive.
 * @returns The Tailwind text color utility to apply to the position badge.
 */
export function resolvePositionBadgeClass(position: number | null): string {
  return (position === null ? undefined : PODIUM_TEXT_CLASSES[position - 1]) ?? DEFAULT_TEXT_CLASS;
}
