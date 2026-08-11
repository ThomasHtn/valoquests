import { AvatarSize } from './avatar.model';

/**
 * Rendering metrics for one {@link AvatarSize}.
 */
interface AvatarSizeMetrics {
  /**
   * Tailwind classes sizing the avatar container. Its silhouette is applied separately, from
   * {@link roundedClass} or {@link AVATAR_HEX_CLASS}, so size and shape can vary independently.
   */
  readonly containerClass: string;

  /**
   * Tailwind corner rounding applied when the avatar is rendered as a `circle`. Scales with the
   * size: the hero portrait is a rounded square rather than a disc.
   */
  readonly roundedClass: string;

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
  xs: {
    containerClass: 'h-6 w-6',
    roundedClass: 'rounded-full',
    iconClass: 'h-3.5 w-3.5',
    pixels: 24,
  },
  sm: { containerClass: 'h-9 w-9', roundedClass: 'rounded-full', iconClass: 'h-5 w-5', pixels: 36 },
  md: {
    containerClass: 'h-11 w-11',
    roundedClass: 'rounded-full',
    iconClass: 'h-6 w-6',
    pixels: 44,
  },
  lg: {
    containerClass: 'h-24 w-24',
    roundedClass: 'rounded-xl',
    iconClass: 'h-12 w-12',
    pixels: 96,
  },
};

/**
 * Silhouette utility for the `hex` shape. It deliberately carries no rounding: the clip *is* the
 * shape, and any leftover radius would round off the hexagon's own corners.
 */
export const AVATAR_HEX_CLASS = 'clip-hex';
