import { TourStepId } from './tour.model';

/**
 * The guided tour's steps, in the order they are walked through.
 *
 * The order is the causal chain of the rescue mission: the goal first (the base, which is the
 * score), then the week that feeds it, what the two resources buy, the challenges, and only
 * afterwards the individual scoreboard. A visitor who meets the ranking first reads everything
 * after it as a competition with a base attached, rather than the other way round.
 */
export const TOUR_STEPS: readonly TourStepId[] = [
  'intro',
  'base',
  'week',
  'resources',
  'challenges',
  'ranking',
];

/**
 * Translation sub-keys of the three figures every step closes on. Fixed at three so the spec sheet
 * reads as one from step to step.
 */
export const TOUR_SPEC_KEYS: readonly string[] = ['spec1', 'spec2', 'spec3'];

/**
 * Marker wrapping the words a step's claim sets in relief, as `*so*`. In the dictionary rather
 * than in markup: the emphasis falls on different words in French and in English.
 */
export const TOUR_EMPHASIS_MARKER = '*';

/**
 * Population a full campaign is expected to reach, the scale the hero's city is drawn on. The
 * overview's own figure.
 */
export const FULL_CAMPAIGN_POPULATION = 30_000;
