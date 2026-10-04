import { LCG_INCREMENT, LCG_MODULUS, LCG_MULTIPLIER } from './seeded-random.constants';
import { RandomSource } from './seeded-random.model';

/**
 * LCG replaying the same sequence for a seed, so drawings match on every visit.
 */
export function createSeededRandom(seed: number): RandomSource {
  let state = seed;
  return () => {
    state = (state * LCG_MULTIPLIER + LCG_INCREMENT) % LCG_MODULUS;
    return state / LCG_MODULUS;
  };
}
