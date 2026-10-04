/**
 * Avatar sizes, from table rows to the profile hero.
 */
export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg';

/**
 * Rendering metrics for one {@link AvatarSize}.
 */
export interface AvatarSizeMetrics {
  /**
   * Tailwind classes sizing the container.
   */
  readonly containerClass: string;

  /**
   * Tailwind classes sizing the fallback icon.
   */
  readonly iconClass: string;

  /**
   * Size in CSS pixels, matching {@link containerClass}; `NgOptimizedImage` requires it.
   */
  readonly pixels: number;
}
