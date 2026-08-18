import { Directive, computed, input } from '@angular/core';

/**
 * Role a button plays in the interface, each mapped to one fixed color treatment below.
 *
 * `accent` and `danger-outline` read as the same weight as `secondary` but tinted, for an action
 * that is not the primary call to action yet still deserves more emphasis than a neutral one (a
 * retry after an error, restoring an archived row) or carries risk without being the dialog's own
 * destructive confirmation (removing a row, as opposed to the dialog that follows it).
 */
export type ButtonVariant =
  'primary' | 'secondary' | 'ghost' | 'accent' | 'danger' | 'danger-outline';

const VARIANT_CLASS: Record<ButtonVariant, string> = {
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

/**
 * Shared chrome for the notched action buttons used across forms, rows and dialogs — cut corner,
 * focus ring, press feedback and disabled treatment — leaving only the color to `appButton`'s
 * variant. Each call site still owns its own height, padding, gap and `--notch` size in a plain
 * `class` attribute, since those track the surrounding layout rather than the button's role.
 *
 * Deliberately not the "way back"/exit chip (`routerLink="/players"` on the player profile, the
 * rules page's replay-tour link): that family is square-edged and reads as navigation rather than
 * as an action, so folding it into this variant set would blur the distinction.
 */
@Directive({
  selector: '[appButton]',
  host: {
    class:
      'notch-tr cursor-pointer focus-ring-inset transition-colors motion-safe:active:scale-[0.96] disabled:cursor-not-allowed disabled:opacity-50',
    '[class]': 'variantClass()',
  },
})
export class Button {
  /**
   * Which color treatment this button renders.
   */
  public readonly appButton = input<ButtonVariant>('secondary');

  /**
   * Resolved Tailwind classes for the current variant.
   */
  protected readonly variantClass = computed(() => VARIANT_CLASS[this.appButton()]);
}
