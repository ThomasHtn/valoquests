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
import { FocusTrap } from '@shared/focus-trap/focus-trap';

/**
 * Modal confirmation for an irreversible backoffice action, optionally gated by a typed phrase.
 * Plain overlay rather than the CDK: always centred, never positioned against a trigger.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [FocusTrap, Button, LucideLoaderCircle, TextField, TextFieldInput],
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
   * Translated dialog title.
   */
  public readonly heading = input.required<string>();

  /**
   * Translated explanation of what confirming does.
   */
  public readonly body = input.required<string>();

  /**
   * Translated label of the confirming button.
   */
  public readonly confirmLabel = input.required<string>();

  /**
   * Translated label of the dismissing button.
   */
  public readonly cancelLabel = input.required<string>();

  /**
   * Phrase to type before confirming, `''` when a click is enough.
   */
  public readonly confirmationPhrase = input('');

  /**
   * Translated hint naming the phrase to type.
   */
  public readonly confirmationHint = input('');

  /**
   * Whether the confirmed action runs, which locks both buttons.
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
   * Panel focused on open so the keyboard lands inside the dialog.
   */
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  /**
   * Phrase typed so far.
   */
  protected readonly typedPhrase = signal('');

  /**
   * Whether confirming is allowed.
   */
  protected readonly canConfirm = computed(() => {
    if (this.busy()) {
      return false;
    }

    const phrase = this.confirmationPhrase();

    return phrase === '' || this.typedPhrase().trim() === phrase;
  });

  /**
   * Focuses the panel on open; clears the phrase on close so a failed action keeps it.
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
   * Records the typed confirmation phrase.
   */
  protected onPhraseInput(event: Event): void {
    this.typedPhrase.set((event.target as HTMLInputElement).value);
  }

  /**
   * Dismisses on Escape unless the action is running.
   */
  protected onEscape(): void {
    if (this.open() && !this.busy()) {
      this.dismissed.emit();
    }
  }
}
