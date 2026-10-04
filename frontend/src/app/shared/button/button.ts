import { Directive, computed, input } from '@angular/core';
import { ButtonVariant } from './button.model';
import { VARIANT_CLASS } from './button.constants';

/**
 * Shared chrome of notched action buttons; call sites own size, padding and `--notch`.
 * Not for square-edged navigation chips, which read as navigation rather than action.
 */
@Directive({
  selector: '[appButton]',
  host: {
    // Disabled goes neutral, not half-transparent: faded amber read as a different button.
    class:
      'notch-tr cursor-pointer focus-ring-inset transition-colors motion-safe:active:scale-[0.96] disabled:cursor-not-allowed disabled:border-edge disabled:bg-surface-700 disabled:text-text-muted disabled:opacity-100 disabled:hover:border-edge disabled:hover:bg-surface-700 disabled:hover:text-text-muted',
    '[class]': 'variantClass()',
  },
})
export class Button {
  /**
   * Color treatment of the button.
   */
  public readonly appButton = input<ButtonVariant>('secondary');

  /**
   * Tailwind classes of the current variant.
   */
  protected readonly variantClass = computed(() => VARIANT_CLASS[this.appButton()]);
}
