import { Component, input, output } from '@angular/core';
import { LucideCircleCheck, LucideLoaderCircle, LucideTriangleAlert } from '@lucide/angular';

import { Button } from '@shared/button/button';
import { AdminActionState } from '../admin-action.model';

/**
 * One triggerable backoffice operation, with its own outcome reported in place.
 *
 * Every maintenance command on these screens has the same anatomy — a name, a sentence saying what
 * it will do, one button, and an answer — so the anatomy is written once here rather than repeated
 * per operation with the drift that invites.
 *
 * The outcome sits inside the card on purpose. These commands take seconds to minutes and are run
 * in sequence when something has gone wrong; a shared banner would overwrite each answer with the
 * next and leave the operator unable to tell which command actually succeeded.
 */
@Component({
  selector: 'app-admin-action-card',
  imports: [Button, LucideCircleCheck, LucideLoaderCircle, LucideTriangleAlert],
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
