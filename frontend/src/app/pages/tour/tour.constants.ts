import { TourStepId } from './tour.model';

/**
 * Tour steps in causal order: the base first, the ranking last so it never reads as the goal.
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
 * Translation sub-keys of the three figures a step closes on.
 */
export const TOUR_SPEC_KEYS: readonly string[] = ['spec1', 'spec2', 'spec3'];

/**
 * Steps without figures: their screen already says it.
 */
export const TOUR_STEPS_WITHOUT_SPECS: readonly TourStepId[] = [
  'resources',
  'challenges',
  'ranking',
];

/**
 * Emphasis marker (`*so*`), in the dictionary since emphasis differs per language.
 */
export const TOUR_EMPHASIS_MARKER = '*';

/**
 * Population a full campaign is expected to reach, the scale of the base step's city.
 */
export const FULL_CAMPAIGN_POPULATION = 30_000;

/**
 * Title key of the page each step's illustration comes from, named under it.
 */
export const TOUR_STEP_SOURCES: Readonly<Record<TourStepId, string>> = {
  intro: 'playerProfile.title',
  base: 'overview.title',
  week: 'overview.title',
  resources: 'overview.title',
  challenges: 'challenges.title',
  ranking: 'leaderboard.title',
};
