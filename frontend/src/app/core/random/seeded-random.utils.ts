/**
 * Deterministic pseudo-random sequence.
 *
 * The drawings (skies, planets, podium) must come back identical on every visit, so they draw
 * from a seeded generator instead of `Math.random()`. A linear congruential generator is enough
 * for scattering a few hundred shapes and needs no dependency.
 */

/**
 * Multiplier, increment and modulus of the Numerical Recipes LCG.
 */
const LCG_MULTIPLIER = 1664525;

/**
 * Increment of the Numerical Recipes LCG.
 */
const LCG_INCREMENT = 1013904223;

/**
 * Modulus of the Numerical Recipes LCG, 2^32.
 */
const LCG_MODULUS = 4294967296;

/**
 * Function yielding the next value of a sequence, uniformly in [0, 1).
 */
export type RandomSource = () => number;

/**
 * Builds a generator that always replays the same sequence for the same seed.
 */
export function createSeededRandom(seed: number): RandomSource {
  let state = seed;
  return () => {
    state = (state * LCG_MULTIPLIER + LCG_INCREMENT) % LCG_MODULUS;
    return state / LCG_MODULUS;
  };
}
