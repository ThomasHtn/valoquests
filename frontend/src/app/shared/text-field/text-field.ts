import { Component, Directive, input } from '@angular/core';

/**
 * Chrome for a labelled text field: the caption above it, and the notched frame the control sits
 * in.
 *
 * The frame is two nested clip-paths rather than a real border. The "border" is the sliver of the
 * outer `div`'s background left showing around an inner one cut to the same polygon but inset by
 * its `p-px` padding. A replaced element like `<input>` never renders the `::after` that
 * `notch-tr-edge` draws the diagonal with, and a rotated line standing in for the cut corner left
 * a hairline gap at its tip — two independently anti-aliased shapes trying to land on the same
 * pixel. Two nested clips of the same polygon never have that seam.
 *
 * The control itself is projected rather than rendered here: the four call sites need their own
 * `type`, validation attributes and event bindings, and forwarding all of them through inputs
 * would be a wider surface than the markup this saves. Put {@link TextFieldInput} on it to pick up
 * the matching typography, and project a trailing button beside it when the field carries one (the
 * backoffice key field's show/hide toggle).
 */
@Component({
  selector: 'app-text-field',
  templateUrl: './text-field.html',
  host: { class: 'block' },
})
export class TextField {
  /**
   * Already-translated caption naming the field.
   */
  public readonly label = input.required<string>();
}

/**
 * Typography and box of the control inside an {@link TextField} frame, which every field repeats
 * identically. Fills the frame's height, stays transparent so the frame's own surface shows
 * through, and drops the user-agent focus outline — the frame turns brand-colored on
 * `focus-within` instead, which is the field's focus indicator.
 */
@Directive({
  selector: 'input[appTextFieldInput]',
  host: {
    class:
      'h-full min-w-0 flex-1 bg-transparent px-3 text-sm text-text-primary placeholder:text-text-muted focus:outline-none',
  },
})
export class TextFieldInput {}
