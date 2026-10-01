import { RankJourneyIconSizes } from './rank-journey.model';

/**
 * Media query under which the chart switches to its compact sizes and labels.
 */
export const RANK_JOURNEY_COMPACT_QUERY = '(max-width: 639px)';

/**
 * Badge and rail sizes on a phone.
 */
export const RANK_JOURNEY_COMPACT_SIZES: RankJourneyIconSizes = { point: 26, peak: 44, rail: 10 };

/**
 * Badge and rail sizes from the `sm` breakpoint up.
 */
export const RANK_JOURNEY_WIDE_SIZES: RankJourneyIconSizes = { point: 34, peak: 56, rail: 14 };

/**
 * How far a rail reaches past the rank it starts or ends on, in divisions, so a season spent on a
 * single rank still shows a short pill rather than nothing.
 */
export const RANK_JOURNEY_RAIL_OVERHANG = 0.3;

/**
 * Fill of the rails, kept below the grid's own weight so the line stays the figure.
 */
export const RANK_JOURNEY_RAIL_COLOR = 'rgb(236 232 225 / 0.1)';

/**
 * Prefix of every translation key the rank journey reads.
 */
export const RANK_JOURNEY_I18N = 'playerProfile.progression.rankJourney';
