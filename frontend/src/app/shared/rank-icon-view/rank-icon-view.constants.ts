import { RankIconSizeMetrics, RankIconSize } from './rank-icon-view.model';

/**
 * Metrics of each rank icon size: sm for dense tables, md by default, lg for hero views.
 */
export const RANK_ICON_SIZES: Readonly<Record<RankIconSize, RankIconSizeMetrics>> = {
  sm: { containerClass: 'h-8 w-8', pixels: 32 },
  md: { containerClass: 'h-12 w-12', pixels: 48 },
  lg: { containerClass: 'h-16 w-16', pixels: 64 },
};
