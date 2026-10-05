import { Directive, input } from '@angular/core';
import { ButtonVariant } from './button.model';

/**
 * Shared chrome of notched action buttons (`styles/components/button.css`); call sites own size,
 * padding and `--notch`. Not for square-edged navigation chips, which read as navigation.
 */
@Directive({
  selector: '[appButton]',
  host: {
    class: 'button notch-tr focus-ring-inset press',
    '[class]': '"button--" + appButton()',
  },
})
export class Button {
  /**
   * Color treatment of the button.
   */
  public readonly appButton = input<ButtonVariant>('secondary');
}
