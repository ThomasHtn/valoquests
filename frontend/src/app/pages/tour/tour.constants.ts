import { TourStepId } from './tour.model';

/**
 * The guided tour's steps, in the order they are walked through.
 *
 * Each entry is reduced to its identifier: it doubles as the translation namespace
 * (`tour.steps.<id>.*`) and as the `@switch` case selecting the live component that illustrates the
 * step, so a step's copy and its visual never drift out of sync through a third mapping.
 *
 * The order is the causal chain, not the sidebar's: the goal first (the town, which is the score),
 * then the week that feeds it, then what a week actually buys, and only afterwards the individual
 * scoreboard. A visitor who meets the ranking first reads everything after it as a competition with
 * a colony attached, rather than the other way round.
 *
 * There is no step for the boss and one for the challenges any more: they were two screens and are
 * now one — `/challenges` redirects to `/week` — so the tour walks the week once. The step they
 * freed goes to the ladder, which is what the whole colony pillar is scored on and which the tour
 * used to skip entirely.
 */
export const TOUR_STEPS: readonly TourStepId[] = [
  'intro',
  'colony',
  'week',
  'ladder',
  'ranking',
  'players',
];

/**
 * Translation sub-keys of the three figures every step closes on.
 *
 * Fixed at three, for every step, on purpose: this is a spec sheet, and a spec sheet that carries
 * two figures on one page and four on the next stops reading as one. Three is also what the copy
 * column fits without a label wrapping past two lines.
 */
export const TOUR_SPEC_KEYS: readonly string[] = ['spec1', 'spec2', 'spec3'];

/**
 * Marker wrapping the words a step's claim sets in relief, as `*so*`.
 *
 * A marker in the dictionary rather than markup: the emphasis falls on different words in French
 * and in English, so it has to travel with the sentence rather than be pinned to a position in a
 * template. Parsed into plain text runs by `Tour.claimRuns`, never through `innerHTML`.
 */
export const TOUR_EMPHASIS_MARKER = '*';
