/**
 * Sky kept above the drawing, in viewBox units: on a phone the frame is cropped to its height, and
 * without it the rocket's tip went under the context bar.
 */
export const SCENE_HEADROOM = 14;

/**
 * `localStorage` key holding the population the overview last showed, so the buildings grown since
 * then rise on the next visit.
 */
export const SEEN_POPULATION_KEY = 'valo-quests.base-seen-population';
