import { Component, computed, input, linkedSignal, output } from '@angular/core';

import { AdminPlayer, AdminPlayerStatus } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Button } from '@shared/button/button';
import { Drawer } from '@shared/drawer/drawer';
import { TextField, TextFieldInput } from '@shared/text-field/text-field';

/**
 * Identity the operator submitted, before it is turned into a create or update request.
 */
export interface PlayerFormResult {
  readonly gameName: string;
  readonly tagLine: string;
  readonly status: AdminPlayerStatus;
}

/**
 * Right-anchored drawer for adding or editing a roster player, opened from a row or from the
 * "Add a player" button.
 *
 * Rendered inside the shared `app-drawer`, which owns the panel and its dismissal. Only the Riot
 * identity (game
 * name, tag) and, when creating, the initial status are editable here — the display name and
 * portrait are not roster-operator concerns day to day, so the panel does not surface them: it
 * carries the display name over unchanged on an edit, and defaults it to the game name on creation.
 */
@Component({
  selector: 'app-player-form-panel',
  imports: [TranslatePipe, Button, Drawer, TextField, TextFieldInput],
  templateUrl: './player-form-panel.html',
})
export class PlayerFormPanel {
  /**
   * The player being edited, or `null` when the panel is adding a new one.
   */
  public readonly editedPlayer = input<AdminPlayer | null>(null);

  /**
   * Whether the submitted command is currently running, which locks the form.
   */
  public readonly busy = input(false);

  /**
   * Emitted when the operator submits a valid form.
   */
  public readonly saved = output<PlayerFormResult>();

  /**
   * Emitted once the drawer has been dismissed, by any means the platform offers (the close
   * button, Escape, or a click on the backdrop).
   */
  public readonly closed = output<void>();

  /**
   * Riot name field, seeded from {@link editedPlayer} and then locally editable.
   */
  protected readonly gameName = linkedSignal(() => this.editedPlayer()?.gameName ?? '');

  /**
   * Tag field, seeded from {@link editedPlayer} and then locally editable.
   */
  protected readonly tagLine = linkedSignal(() => this.editedPlayer()?.tagLine ?? '');

  /**
   * Status the new player will be created with. Not offered when editing: an existing player's
   * status is changed from its own row, so offering it here too would give the same state two
   * owners.
   */
  protected readonly status = linkedSignal<AdminPlayerStatus>(
    () => this.editedPlayer()?.status ?? 'ACTIVE',
  );

  /**
   * Whether the form holds enough to be submitted.
   */
  protected readonly valid = computed(
    () => this.gameName().trim() !== '' && this.tagLine().trim() !== '',
  );

  /**
   * Updates the Riot name field.
   *
   * @param event - The input event carrying the field's current value.
   */
  protected onGameNameInput(event: Event): void {
    this.gameName.set((event.target as HTMLInputElement).value);
  }

  /**
   * Updates the tag field.
   *
   * @param event - The input event carrying the field's current value.
   */
  protected onTagLineInput(event: Event): void {
    this.tagLine.set((event.target as HTMLInputElement).value);
  }

  /**
   * Sets the status the new player will be created with.
   *
   * @param status - The status to apply.
   */
  protected onStatusChange(status: AdminPlayerStatus): void {
    this.status.set(status);
  }

  /**
   * Emits the form's contents, once validated.
   *
   * @param event - The form submission, whose default navigation is prevented.
   */
  protected submit(event: Event): void {
    event.preventDefault();

    if (!this.valid() || this.busy()) {
      return;
    }

    this.saved.emit({
      gameName: this.gameName().trim(),
      tagLine: this.tagLine().trim(),
      status: this.status(),
    });
  }
}
