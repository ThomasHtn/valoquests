/**
 * Size preset for a rank icon.
 */
export type RankIconSize = 'sm' | 'md' | 'lg';

/**
 * Rendering metrics for a rank icon at a given size: CSS class and pixel dimensions.
 */
export interface RankIconSizeMetrics {
  readonly containerClass: string;
  readonly pixels: number;
}
