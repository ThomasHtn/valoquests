import { DEFAULT_TEXT_CLASS, PODIUM_TEXT_CLASSES } from './ranking-visual.constants';

/**
 * Tailwind text color of a position badge; `null` (inactive) stays neutral.
 */
export function resolvePositionBadgeClass(position: number | null): string {
  return (position === null ? undefined : PODIUM_TEXT_CLASSES[position - 1]) ?? DEFAULT_TEXT_CLASS;
}
