import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';

import { AdminPlayer, AdminPlayerStatus } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { AGENT_PORTRAIT_FILES, resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { Drawer } from '@shared/drawer/drawer';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { TextField, TextFieldInput } from '@shared/text-field/text-field';

/**
 * Value the portrait dropdown holds when no avatar is associated.
 *
 * The dropdown carries the "no avatar" case as an option of its own rather than as an unset value:
 * `app-select` renders an unset value as an icon-only trigger, which would read as "nothing chosen
 * yet" instead of the deliberate choice it is here.
 */
const NO_PORTRAIT = '';

/**
 * Identity the operator submitted, before it is turned into a create or update request.
 */
export interface PlayerFormResult {
  readonly gameName: string;
  readonly tagLine: string;

  /**
   * Agent name backing the bundled avatar, or `null` when none was chosen.
   */
  readonly portrait: string | null;
  readonly status: AdminPlayerStatus;
}

/**
 * Right-anchored drawer for adding or editing a roster player, opened from a row or from the
 * "Add a player" button.
 *
 * Rendered inside the shared `app-drawer`, which owns the panel and its dismissal. The Riot
 * identity (game name, tag), the avatar and, when creating, the initial status are editable here —
 * the display name is not a roster-operator concern day to day, so the panel does not surface it:
 * it carries it over unchanged on an edit, and defaults it to the game name on creation.
 */
@Component({
  selector: 'app-player-form-panel',
  imports: [
    TranslatePipe,
    Avatar,
    Button,
    Drawer,
    LucideLoaderCircle,
    Select,
    TextField,
    TextFieldInput,
  ],
  templateUrl: './player-form-panel.html',
})
export class PlayerFormPanel {
  /**
   * i18n service used to build the portrait options outside the template.
   */
  private readonly translation = inject(Translation);

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
   * Agent name backing the player's avatar, seeded from {@link editedPlayer} and then locally
   * editable. Holds {@link NO_PORTRAIT} when no avatar is associated.
   */
  protected readonly portrait = linkedSignal(() => this.editedPlayer()?.portrait ?? NO_PORTRAIT);

  /**
   * Avatars the dropdown offers: the bundled agent portraits, plus the "no avatar" option.
   */
  protected readonly portraitOptions = computed<readonly SelectOption<string>[]>(() => [
    { value: NO_PORTRAIT, label: this.translation.translate('admin.players.form.portraitNone') },
    ...AGENT_PORTRAIT_FILES.map((agent) => ({ value: agent, label: agent })),
  ]);

  /**
   * Asset the preview beside the dropdown renders, or `null` for the fallback icon.
   */
  protected readonly portraitPreview = computed(() => resolvePlayerAvatarUrl(this.portrait()));

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
   * Sets the avatar associated with the player.
   *
   * @param portrait - The chosen agent name, {@link NO_PORTRAIT} for none. Never `null`, since the
   * dropdown only offers the options built above, but typed as the select emits it.
   */
  protected onPortraitChange(portrait: string | null): void {
    this.portrait.set(portrait ?? NO_PORTRAIT);
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
      portrait: this.portrait() === NO_PORTRAIT ? null : this.portrait(),
      status: this.status(),
    });
  }
}
