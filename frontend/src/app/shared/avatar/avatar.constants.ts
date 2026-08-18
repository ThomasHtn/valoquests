import { AvatarSize } from './avatar.model';

/**
 * Rendering metrics for one {@link AvatarSize}.
 */
interface AvatarSizeMetrics {
  /**
   * Tailwind classes sizing the avatar container.
   */
  readonly containerClass: string;

  /**
   * Tailwind classes sizing the fallback icon.
   */
  readonly iconClass: string;

  /**
   * Rendered size in CSS pixels, matching {@link containerClass}.
   *
   * Declared explicitly because `NgOptimizedImage` requires intrinsic dimensions to reserve
   * layout space, which avoids a layout shift while the portrait loads.
   */
  readonly pixels: number;
}

/**
 * Rendering metrics for each {@link AvatarSize}.
 */
export const AVATAR_SIZES: Readonly<Record<AvatarSize, AvatarSizeMetrics>> = {
  xs: { containerClass: 'h-6 w-6', iconClass: 'h-3.5 w-3.5', pixels: 24 },
  sm: { containerClass: 'h-9 w-9', iconClass: 'h-5 w-5', pixels: 36 },
  md: { containerClass: 'h-16 w-16', iconClass: 'h-8 w-8', pixels: 64 },
  lg: { containerClass: 'h-24 w-24', iconClass: 'h-12 w-12', pixels: 96 },
};
