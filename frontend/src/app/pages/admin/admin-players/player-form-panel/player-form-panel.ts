import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';
import { AdminPlayer, AdminPlayerStatus } from '@core/admin/players/admin-player.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { AGENT_PORTRAIT_FILES } from '@core/players/avatar/player-avatar.constants';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { Drawer } from '@shared/drawer/drawer';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { TextField, TextFieldInput } from '@shared/text-field/text-field';
import { NO_PORTRAIT } from './player-form-panel.constants';
import { PlayerFormResult } from './player-form-panel.model';

/**
 * Drawer to add or edit a roster player; the display name is not edited here.
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
   * Translation, for the "no portrait" option label.
   */
  private readonly translation = inject(Translation);

  /**
   * Edited player, `null` when adding.
   */
  public readonly editedPlayer = input<AdminPlayer | null>(null);

  /**
   * Whether the submitted command is running, which locks the form.
   */
  public readonly busy = input(false);

  /**
   * Emitted on a valid submission.
   */
  public readonly saved = output<PlayerFormResult>();

  /**
   * Emitted once the drawer is dismissed by any means.
   */
  public readonly closed = output<void>();

  /**
   * Game name field, seeded from {@link editedPlayer}.
   */
  protected readonly gameName = linkedSignal(() => this.editedPlayer()?.gameName ?? '');

  /**
   * Tag field, seeded from {@link editedPlayer}.
   */
  protected readonly tagLine = linkedSignal(() => this.editedPlayer()?.tagLine ?? '');

  /**
   * Portrait agent name, seeded from {@link editedPlayer}, {@link NO_PORTRAIT} for none.
   */
  protected readonly portrait = linkedSignal(() => this.editedPlayer()?.portrait ?? NO_PORTRAIT);

  /**
   * Portrait options: bundled agents plus "no avatar".
   */
  protected readonly portraitOptions = computed<readonly SelectOption<string>[]>(() => [
    { value: NO_PORTRAIT, label: this.translation.translate('admin.players.form.portraitNone') },
    ...AGENT_PORTRAIT_FILES.map((agent) => ({ value: agent, label: agent })),
  ]);

  /**
   * Preview URL, `null` for the fallback icon.
   */
  protected readonly portraitPreview = computed(() => resolvePlayerAvatarUrl(this.portrait()));

  /**
   * Initial status of a new player, not offered when editing.
   */
  protected readonly status = linkedSignal<AdminPlayerStatus>(
    () => this.editedPlayer()?.status ?? 'ACTIVE',
  );

  /**
   * Whether the form can be submitted.
   */
  protected readonly valid = computed(
    () => this.gameName().trim() !== '' && this.tagLine().trim() !== '',
  );

  /**
   * Updates the game name field.
   */
  protected onGameNameInput(event: Event): void {
    this.gameName.set((event.target as HTMLInputElement).value);
  }

  /**
   * Updates the tag field.
   */
  protected onTagLineInput(event: Event): void {
    this.tagLine.set((event.target as HTMLInputElement).value);
  }

  /**
   * Sets the portrait; `null` never comes but is what the select emits.
   */
  protected onPortraitChange(portrait: string | null): void {
    this.portrait.set(portrait ?? NO_PORTRAIT);
  }

  /**
   * Sets the initial status.
   */
  protected onStatusChange(status: AdminPlayerStatus): void {
    this.status.set(status);
  }

  /**
   * Emits the trimmed form contents once valid.
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
