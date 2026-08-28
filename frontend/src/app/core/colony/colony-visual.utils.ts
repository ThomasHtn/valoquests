import { ColonyDeltaView } from './colony-view.model';

/**
 * Text color for a delta figure (population arrivals, run history growth), by the direction it
 * moved — the same rule the campaign page's tiles and the overview's population tile share.
 *
 * @param delta - What the night moved.
 * @returns The colour utility.
 */
export function resolveColonyDeltaColorClass(delta: ColonyDeltaView): string {
  return delta.isPositive ? 'text-success' : delta.isNegative ? 'text-danger' : 'text-text-muted';
}
