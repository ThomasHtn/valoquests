import { RankIconSizeMetrics, RankIconSize } from './rank-icon-view.model';

/**
 * Rendering metrics for each rank icon size preset, matching the adopted sizes across the
 * application's tier displays: sm for dense tables, md for balanced spacing, lg for hero or
 * detail views.
 */
export const RANK_ICON_SIZES: Readonly<Record<RankIconSize, RankIconSizeMetrics>> = {
  sm: { containerClass: 'h-8 w-8', pixels: 32 },
  md: { containerClass: 'h-12 w-12', pixels: 48 },
  lg: { containerClass: 'h-16 w-16', pixels: 64 },
};
