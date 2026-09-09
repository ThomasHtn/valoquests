import { Directive, computed, input } from '@angular/core';
import { ButtonVariant } from './button.model';
import { VARIANT_CLASS } from './button.constants';

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
    // Disabled buttons go inert rather than half-transparent. `opacity-50` over a dark ground turned
    // the amber fill into a muddy brown that read as a *different* button rather than as the same
    // one switched off — two "Synchroniser" buttons side by side in the backoffice came out in two
    // colours. Neutralising the fill and the label instead keeps one shape and one meaning, and the
    // `disabled:` pseudo-class outranks the variant's own colour without needing `!important`.
    class:
      'notch-tr cursor-pointer focus-ring-inset transition-colors motion-safe:active:scale-[0.96] disabled:cursor-not-allowed disabled:border-edge disabled:bg-surface-700 disabled:text-text-muted disabled:opacity-100 disabled:hover:border-edge disabled:hover:bg-surface-700 disabled:hover:text-text-muted',
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
