import { Component, input, output } from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';

import { AdminActionState } from '@core/admin/admin-action.model';
import { Button } from '@shared/button/button';

/**
 * One triggerable backoffice operation.
 *
 * Every maintenance command on these screens has the same anatomy — a name, a sentence saying what
 * it will do, and one button — so the anatomy is written once here rather than repeated per
 * operation with the drift that invites. Its outcome is reported through the global snackbar, not
 * in the card itself: {@link state} is still consumed for the running spinner and to keep the
 * button disabled for the command's duration.
 */
@Component({
  selector: 'app-admin-action-card',
  imports: [Button, LucideLoaderCircle],
  templateUrl: './admin-action-card.html',
  host: { class: 'block' },
})
export class AdminActionCard {
  /**
   * Already-translated operation name.
   */
  public readonly heading = input.required<string>();

  /**
   * Already-translated sentence describing what triggering the operation does.
   */
  public readonly description = input.required<string>();

  /**
   * Already-translated button label.
   */
  public readonly actionLabel = input.required<string>();

  /**
   * Current state of the operation.
   */
  public readonly state = input.required<AdminActionState>();

  /**
   * Whether the button is unavailable for a reason of the page's own, beyond the operation already
   * running.
   */
  public readonly disabled = input(false);

  /**
   * Whether the operation destroys data, which switches the button to danger tones.
   */
  public readonly destructive = input(false);

  /**
   * Emitted when the operator triggers the operation.
   */
  public readonly triggered = output<void>();
}
