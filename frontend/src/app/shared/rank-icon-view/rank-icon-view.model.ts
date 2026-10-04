/**
 * Size preset of a rank icon.
 */
export type RankIconSize = 'sm' | 'md' | 'lg';

/**
 * Rendering metrics of a rank icon size.
 */
export interface RankIconSizeMetrics {
  /**
   * Tailwind size classes of the container.
   */
  readonly containerClass: string;

  /**
   * Rendered size in pixels.
   */
  readonly pixels: number;
}
