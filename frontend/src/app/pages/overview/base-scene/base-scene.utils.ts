import { SEEN_POPULATION_KEY } from './base-scene.constants';

/**
 * Convenience only: losing storage skips the building rise, never the drawing.
 */

/**
 * Population last shown in this browser, `null` if unknown.
 */
export function readSeenPopulation(): number | null {
  try {
    const stored = Number(localStorage.getItem(SEEN_POPULATION_KEY));
    return Number.isFinite(stored) && stored > 0 ? stored : null;
  } catch {
    return null;
  }
}

/**
 * Records the population just shown.
 */
export function writeSeenPopulation(population: number): void {
  try {
    localStorage.setItem(SEEN_POPULATION_KEY, String(population));
  } catch {
    // Storage unavailable: the next visit draws without the rise.
  }
}
