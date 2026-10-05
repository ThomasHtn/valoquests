import { RankIconSize } from './rank-icon-view.model';

/**
 * Rendered side in pixels of each rank icon size: sm for dense tables, md by default, lg for hero views.
 */
export const RANK_ICON_PIXELS: Readonly<Record<RankIconSize, number>> = {
  sm: 32,
  md: 48,
  lg: 64,
};
