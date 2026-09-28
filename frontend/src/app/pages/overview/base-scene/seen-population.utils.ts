import { SEEN_POPULATION_KEY } from './base-scene.constants';

/**
 * Memory of the last population the overview drew, per browser.
 *
 * Only a convenience: a private window, cleared storage or a blocked access simply lose the rise
 * of the new buildings, never the drawing itself.
 */

/**
 * Population the overview last showed in this browser, or null if unknown.
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
 * Records the population the overview just showed.
 */
export function writeSeenPopulation(population: number): void {
  try {
    localStorage.setItem(SEEN_POPULATION_KEY, String(population));
  } catch {
    // Storage unavailable: the next visit draws without the rise.
  }
}
