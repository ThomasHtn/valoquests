import { AvatarSize } from './avatar.model';

/**
 * Rendered side of each {@link AvatarSize} in CSS pixels, matching `avatar.scss`; `NgOptimizedImage` requires it.
 */
export const AVATAR_PIXELS: Readonly<Record<AvatarSize, number>> = {
  xs: 24,
  sm: 36,
  md: 64,
  lg: 96,
};
