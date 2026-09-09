import { NavChipVariant } from './nav-chip.model';

/**
 * Tailwind classes of each chip variant.
 */
export const VARIANT_CLASS: Record<NavChipVariant, string> = {
  outline:
    'px-4 font-medium text-text-muted hover:text-text-primary disabled:cursor-default disabled:opacity-35 disabled:hover:text-text-muted',
  solid: 'bg-brand-500 px-5 font-bold text-surface-950 hover:bg-brand-400',
};
