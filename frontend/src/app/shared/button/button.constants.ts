import { ButtonVariant } from './button.model';

/**
 * Tailwind classes of each button variant.
 */
export const VARIANT_CLASS: Record<ButtonVariant, string> = {
  primary: 'bg-brand-500 text-surface-950 hover:bg-brand-400',
  secondary:
    'notch-tr-edge border border-text-secondary/40 text-text-secondary hover:border-brand-400/70 hover:text-brand-400',
  ghost: 'text-text-secondary hover:bg-brand-500/8 hover:text-text-primary',
  accent:
    'notch-tr-edge border border-brand-500/45 bg-brand-500/12 text-brand-400 hover:border-brand-500/70 hover:bg-brand-500/20',
  danger: 'bg-danger text-surface-950 hover:bg-danger/90',
  'danger-outline':
    'notch-tr-edge border border-danger/40 text-danger hover:border-danger/70 hover:bg-danger/12',
};
