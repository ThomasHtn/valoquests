import { PositionBadgeSize } from './position-badge.model';

/**
 * Rendering metrics for one {@link PositionBadgeSize}.
 */
interface PositionBadgeSizeMetrics {
  /**
   * Tailwind classes sizing the badge container.
   */
  readonly containerClass: string;

  /**
   * Tailwind class sizing the position number.
   */
  readonly textClass: string;
}

/**
 * Rendering metrics for each {@link PositionBadgeSize}.
 */
export const POSITION_BADGE_SIZES: Readonly<Record<PositionBadgeSize, PositionBadgeSizeMetrics>> = {
  sm: { containerClass: 'h-6 w-6', textClass: 'text-xs' },
  md: { containerClass: 'h-8 w-8', textClass: 'text-sm' },
};
