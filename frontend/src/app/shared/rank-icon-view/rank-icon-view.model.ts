/**
 * Size preset for a rank icon.
 */
export type RankIconSize = 'sm' | 'md' | 'lg';

/**
 * Rendering metrics for a rank icon at a given size: CSS class and pixel dimensions.
 */
export interface RankIconSizeMetrics {
  /**
   * Tailwind size classes of the container.
   */
  readonly containerClass: string;

  /**
   * Rendered size, in pixels.
   */
  readonly pixels: number;
}
