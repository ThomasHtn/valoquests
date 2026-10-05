import { Component, computed, input, output } from '@angular/core';

import { AdminActionState } from '@core/admin/commands/admin-action.model';
import { Button } from '@shared/button/button';
import { ButtonVariant } from '@shared/button/button.model';
import { Spinner } from '@shared/spinner/spinner';

/**
 * One backoffice operation: name, description, button; the outcome goes to the snackbar.
 */
@Component({
  selector: 'app-admin-action-card',
  imports: [Button, Spinner],
  templateUrl: './admin-action-card.html',
  styleUrl: './admin-action-card.scss',
  host: { class: 'block' },
})
export class AdminActionCard {
  /**
   * Translated operation name.
   */
  public readonly heading = input.required<string>();

  /**
   * Translated description of what the operation does.
   */
  public readonly description = input.required<string>();

  /**
   * Translated button label.
   */
  public readonly actionLabel = input.required<string>();

  /**
   * Operation state, for the spinner and the disabled button.
   */
  public readonly state = input.required<AdminActionState>();

  /**
   * Whether the page disables the button, beyond a running operation.
   */
  public readonly disabled = input(false);

  /**
   * Whether the operation destroys data (danger tones).
   */
  public readonly destructive = input(false);

  /**
   * Emitted when the operator triggers the operation.
   */
  public readonly triggered = output<void>();

  /**
   * Whether the operation is in flight, for the spinner.
   */
  protected readonly running = computed(() => this.state().status === 'running');

  /**
   * Button tone, danger for a destructive operation.
   */
  protected readonly buttonVariant = computed<ButtonVariant>(() =>
    this.destructive() ? 'danger' : 'primary',
  );
}
