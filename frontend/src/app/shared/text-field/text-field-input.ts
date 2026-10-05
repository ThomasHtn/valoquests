import { Directive } from '@angular/core';

/**
 * Styles the control inside a `TextField` (`styles/components/text-field-input.css`).
 */
@Directive({
  selector: 'input[appTextFieldInput]',
  host: { class: 'text-field-input' },
})
export class TextFieldInput {}
