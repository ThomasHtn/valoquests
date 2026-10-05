import { Component, input } from '@angular/core';

/**
 * Caption and notched frame around a projected control (mark it with `appTextFieldInput`).
 * The border is two nested clip-paths: `<input>` has no `::after` for `notch-tr-edge`.
 */
@Component({
  selector: 'app-text-field',
  templateUrl: './text-field.html',
  styleUrl: './text-field.scss',
  host: { class: 'block' },
})
export class TextField {
  /**
   * Translated caption naming the field.
   */
  public readonly label = input.required<string>();
}
