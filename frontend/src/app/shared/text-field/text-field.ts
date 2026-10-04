import { Component, Directive, input } from '@angular/core';

/**
 * Caption and notched frame around a projected control (mark it with {@link TextFieldInput}).
 * The border is two nested clip-paths: `<input>` has no `::after` for `notch-tr-edge`.
 */
@Component({
  selector: 'app-text-field',
  templateUrl: './text-field.html',
  host: { class: 'block' },
})
export class TextField {
  /**
   * Translated caption naming the field.
   */
  public readonly label = input.required<string>();
}

/**
 * Styles the control inside a {@link TextField}; the frame's `focus-within` is its focus ring.
 */
@Directive({
  selector: 'input[appTextFieldInput]',
  host: {
    class:
      'h-full min-w-0 flex-1 bg-transparent px-3 text-base text-text-primary placeholder:text-text-muted focus:outline-none sm:text-sm',
  },
})
export class TextFieldInput {}
