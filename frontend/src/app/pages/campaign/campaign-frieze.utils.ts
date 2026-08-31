/**
 * One star of the frieze's night ground, in the plot's own 1000 × 400 box.
 */
export interface FriezeStar {
  readonly x: number;
  readonly y: number;
  readonly r: number;
  readonly o: number;
}

/**
 * How many stars the field holds. Enough to read as a sky, few enough that the frieze stays a few
 * dozen nodes rather than a few hundred — the block already draws ten markers and ten cards.
 */
const STAR_COUNT = 46;

/**
 * Multiplier and modulus of the field's own generator.
 *
 * A fixed sequence rather than `Math.random`: the stars have to fall in the same places on every
 * render, or a change detection pass would visibly shuffle the sky. Written out rather than pulled
 * from a library — this is four lines, and the values are the classic minimal standard ones.
 */
const RANDOM_MULTIPLIER = 48271;
const RANDOM_MODULUS = 2147483647;
const RANDOM_SEED = 20260831;

/**
 * Builds the frieze's star field, identical on every call.
 *
 * @returns The stars, in drawing order.
 */
export function buildFriezeStars(): readonly FriezeStar[] {
  let state = RANDOM_SEED;
  const next = (): number => {
    state = (state * RANDOM_MULTIPLIER) % RANDOM_MODULUS;
    return state / RANDOM_MODULUS;
  };

  return Array.from({ length: STAR_COUNT }, () => ({
    x: Number((next() * 1000).toFixed(0)),
    y: Number((next() * 400).toFixed(0)),
    r: Number((0.9 + next() * 0.6).toFixed(1)),
    o: Number((0.14 + next() * 0.36).toFixed(2)),
  }));
}

/**
 * Where the rail stops being solid, as a share of its own width.
 *
 * Ten cells of equal width put marker `i` at `(i + 0.5) / count`, so the split is the centre of the
 * week being fought. A run whose weeks have all settled has no such marker: the rail is then solid
 * all the way, since there is no road left to promise.
 *
 * @param currentIndex - Position of the week being fought, or `-1` when none is.
 * @param count - How many weeks the run holds.
 * @returns The split, in `[0, 100]`.
 */
export function railSplitPercentage(currentIndex: number, count: number): number {
  if (count === 0) {
    return 0;
  }

  if (currentIndex < 0) {
    return 100;
  }

  return Number((((currentIndex + 0.5) / count) * 100).toFixed(2));
}
