/**
 * Text color applied to a ranking row's position badge, from 1st place to the neutral treatment
 * used from 4th place onward. Drives the badge's hexagon fill and stroke too, through
 * `currentColor`.
 *
 * Third place uses its own bronze token rather than `accent-red`: that hue is the exact value of
 * `--color-danger`, so a podium place was reading as an error state.
 */
const PODIUM_TEXT_CLASSES: readonly string[] = [
  'text-accent-gold',
  'text-text-secondary',
  'text-podium-bronze',
];

/**
 * Default badge treatment for positions outside the podium.
 */
const DEFAULT_TEXT_CLASS = 'text-text-secondary';

/**
 * Resolves the text color for a ranking row's position badge, highlighting the podium (1st to
 * 3rd) with a distinct color per rank. `null` (an inactive player, who never consumes a ranking
 * slot) gets the same neutral treatment as any position outside the podium.
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
