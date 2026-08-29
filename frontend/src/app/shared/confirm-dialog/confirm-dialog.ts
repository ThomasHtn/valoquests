import {
  Component,
  computed,
  effect,
  ElementRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';

import { Button } from '@shared/button/button';
import { TextField, TextFieldInput } from '@shared/text-field/text-field';

/**
 * Modal confirmation for an action that cannot be undone.
 *
 * Exists because the backoffice is the one place in this application where a click destroys data
 * the API cannot give back. The dialog is deliberately not reusable as a generic "are you sure":
 * it can demand that a phrase be typed out, which is what separates an action the operator meant
 * from one they reached by muscle memory.
 *
 * Rendered as a plain overlay rather than through the CDK: it is always centred, always modal, and
 * never needs to be positioned against a trigger, so an overlay container would add a dependency
 * for behaviour this does not use. Focus is moved into the dialog on open and Escape dismisses it.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [Button, LucideLoaderCircle, TextField, TextFieldInput],
  templateUrl: './confirm-dialog.html',
  host: {
    class: 'contents',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class ConfirmDialog {
  /**
   * Whether the dialog is on screen.
   */
  public readonly open = input.required<boolean>();

  /**
   * Already-translated dialog title.
   */
  public readonly heading = input.required<string>();

  /**
   * Already-translated explanation of what confirming will do.
   */
  public readonly body = input.required<string>();

  /**
   * Already-translated label of the confirming button.
   */
  public readonly confirmLabel = input.required<string>();

  /**
   * Already-translated label of the dismissing button.
   */
  public readonly cancelLabel = input.required<string>();

  /**
   * Phrase the operator must type before confirming, or `''` when a click is enough.
   *
   * Reserved for the irreversible operations: typing the phrase cannot be done by accident, which
   * a second click can.
   */
  public readonly confirmationPhrase = input('');

  /**
   * Already-translated hint naming the phrase to type, shown only when one is required.
   */
  public readonly confirmationHint = input('');

  /**
   * Whether the confirmed action is currently running, which locks both buttons.
   */
  public readonly busy = input(false);

  /**
   * Emitted when the operator confirms.
   */
  public readonly confirmed = output<void>();

  /**
   * Emitted when the operator dismisses the dialog.
   */
  public readonly dismissed = output<void>();

  /**
   * Dialog panel, focused on open so the keyboard lands inside the dialog rather than on the page
   * behind it.
   */
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  /**
   * What the operator has typed so far, when a phrase is required.
   */
  protected readonly typedPhrase = signal('');

  /**
   * Whether confirming is currently allowed.
   */
  protected readonly canConfirm = computed(() => {
    if (this.busy()) {
      return false;
    }

    const phrase = this.confirmationPhrase();

    return phrase === '' || this.typedPhrase().trim() === phrase;
  });

  /**
   * Moves focus into the dialog when it opens, and clears the typed phrase when it closes.
   *
   * Clearing on close rather than on open matters for the failure path: a dialog dismissed and
   * reopened must ask again, and one left open by a failed action must keep what was typed.
   */
  constructor() {
    effect(() => {
      if (this.open()) {
        this.panel()?.nativeElement.focus();
      } else {
        this.typedPhrase.set('');
      }
    });
  }

  /**
   * Records what the operator typed into the confirmation field.
   *
   * @param event - The input event carrying the field's current value.
   */
  protected onPhraseInput(event: Event): void {
    this.typedPhrase.set((event.target as HTMLInputElement).value);
  }

  /**
   * Dismisses the dialog on Escape, unless the confirmed action is already running.
   */
  protected onEscape(): void {
    if (this.open() && !this.busy()) {
      this.dismissed.emit();
    }
  }
}
