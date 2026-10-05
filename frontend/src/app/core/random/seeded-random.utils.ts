import { LCG_INCREMENT, LCG_MODULUS, LCG_MULTIPLIER } from './seeded-random.constants';
import { RandomSource } from './seeded-random.model';

/**
 * LCG replaying the same sequence for a seed, so drawings match on every visit.
 */
export function createSeededRandom(seed: number): RandomSource {
  // Brought into [0, 2^32) as an integer: `%` keeps the sign and fractions would never cycle.
  let state = ((Math.trunc(seed) % LCG_MODULUS) + LCG_MODULUS) % LCG_MODULUS;
  return () => {
    state = (state * LCG_MULTIPLIER + LCG_INCREMENT) % LCG_MODULUS;
    return state / LCG_MODULUS;
  };
}
