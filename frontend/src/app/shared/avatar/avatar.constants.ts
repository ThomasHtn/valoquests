import { AvatarSize } from './avatar.model';

/**
 * Container and fallback-icon classes for each {@link AvatarSize}.
 */
export const AVATAR_SIZE_CLASSES: Readonly<
  Record<AvatarSize, { readonly container: string; readonly icon: string }>
> = {
  sm: { container: 'h-9 w-9 rounded-full', icon: 'h-5 w-5' },
  md: { container: 'h-11 w-11 rounded-full', icon: 'h-6 w-6' },
  lg: { container: 'h-24 w-24 rounded-xl', icon: 'h-12 w-12' },
};
