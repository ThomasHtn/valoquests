import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

import { SEEN_POPULATION_KEY } from './base-scene.constants';

/**
 * Population last shown in this browser, `null` if unknown or storage is unavailable.
 */
export function readSeenPopulation(): number | null {
  const stored = Number(readStorage(SEEN_POPULATION_KEY));
  return Number.isFinite(stored) && stored > 0 ? stored : null;
}

/**
 * Records the population just shown; losing it only skips the next building rise.
 */
export function writeSeenPopulation(population: number): void {
  writeStorage(SEEN_POPULATION_KEY, String(population));
}
