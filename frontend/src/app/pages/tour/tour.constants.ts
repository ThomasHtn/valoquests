import { TourStepId } from './tour.model';

/**
 * The guided tour's steps, in the order they are walked through.
 *
 * Each entry is reduced to its identifier: it doubles as the translation namespace
 * (`tour.steps.<id>.*`) and as the `@switch` case selecting the live component that illustrates the
 * step, so a step's copy and its visual never drift out of sync through a third mapping.
 *
 * The order follows the week's own loop — the boss to bring down, the challenges that damage it,
 * the colony all of it feeds, the ranking that comes out of it — rather than the sidebar's
 * navigation order, which opens on a summary rather than on the beginning of the story.
 *
 * The colony comes before the ranking on purpose: it is the collective goal the whole product is
 * built around, and a visitor who meets the individual scoreboard first reads everything after it
 * as a competition with a colony attached, rather than the other way round.
 */
export const TOUR_STEPS: readonly TourStepId[] = [
  'intro',
  'boss',
  'challenges',
  'colony',
  'ranking',
  'players',
];
