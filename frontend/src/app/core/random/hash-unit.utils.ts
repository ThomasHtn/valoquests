/**
 * Stateless randomness: the same keys always give the same draw, whatever was drawn before.
 *
 * A seeded sequence shifts every later draw as soon as one more is taken; a drawing whose content
 * grows (a building more, a window more) would then reshuffle everything else on screen.
 */

/**
 * Hashes a list of integers into a draw in [0, 1).
 *
 * @param keys - Integers identifying the draw (an id, an index, a salt...).
 * @returns A value in [0, 1), stable for the same keys.
 */
export function hashUnit(...keys: readonly number[]): number {
  let h = 0x811c9dc5;
  for (const key of keys) {
    h = Math.imul(h ^ (key | 0), 0x01000193);
    h ^= h >>> 15;
  }
  h = Math.imul(h ^ (h >>> 16), 0x85ebca6b);
  h = Math.imul(h ^ (h >>> 13), 0xc2b2ae35);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}
