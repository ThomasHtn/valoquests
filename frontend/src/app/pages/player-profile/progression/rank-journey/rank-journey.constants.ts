import { LucideArrowDownUp, LucideTrophy } from '@lucide/angular';

import { KEY_FIGURE_ICONS } from '../key-figures/key-figures.constants';
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
 * Rail overhang past its end ranks, in divisions, so a single-rank season still shows a pill.
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

/**
 * Icons of the tooltip's lines: the key figures' pictograms (delta arrows, matches), range and wins.
 */
export const RANK_JOURNEY_TOOLTIP_ICONS = {
  ...KEY_FIGURE_ICONS,
  range: LucideArrowDownUp,
  wins: LucideTrophy,
} as const;
