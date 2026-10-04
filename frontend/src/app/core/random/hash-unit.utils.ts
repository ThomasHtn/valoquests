/**
 * Stable draw in [0, 1) for the same keys; unlike a sequence, an extra draw reshuffles nothing.
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
