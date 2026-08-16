import { Component, computed, inject, signal } from '@angular/core';
import { LucidePower, LucideRotateCcw, LucideSquarePen, LucideTrash2 } from '@lucide/angular';

import { AdminApi } from '@core/admin/admin-api';
import { resolveAdminErrorMessage } from '@core/admin/admin-error.utils';
import { AdminPlayer, AdminPlayerStatus } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { formatSynchronizationTimestamp } from '@layout/sidebar/sidebar.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { AdminActionState, IDLE_ACTION } from '../admin-action.model';
import { PlayerFormPanel, PlayerFormResult } from './player-form-panel/player-form-panel';

/**
 * Backoffice roster screen.
 *
 * Adds, edits, activates, deactivates and removes tracked players. Removing is the one operation
 * whose result the screen cannot predict: a player that took part in the campaign is archived
 * instead of deleted, so that finalized weeks crediting it with a boss kill stay readable. The
 * table says which fate awaits each player before the operator asks, and the confirmation says
 * which one actually happened.
 */
@Component({
  selector: 'app-admin-players',
  imports: [
    TranslatePipe,
    Button,
    ConfirmDialog,
    PlayerFormPanel,
    ResourceState,
    SectionDivider,
    LucidePower,
    LucideRotateCcw,
    LucideSquarePen,
    LucideTrash2,
  ],
  templateUrl: './admin-players.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminPlayers {
  /**
   * Data-access service backing the roster resource and every command.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * i18n service used to resolve outcome messages built outside templates.
   */
  private readonly translation = inject(Translation);

  /**
   * Resource holding every player, archived ones included.
   */
  protected readonly playersResource = this.adminApi.players;

  /**
   * Placeholder line widths of the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Every player, ordered as the backend returns them.
   */
  protected readonly players = computed(() => resourceValue(this.playersResource, []));

  /**
   * Player currently being edited, or `null` when the form is adding a new one.
   */
  protected readonly editedPlayer = signal<AdminPlayer | null>(null);

  /**
   * Whether the roster form panel is on screen.
   */
  protected readonly formOpen = signal(false);

  /**
   * State of the last roster command, reported above the table.
   */
  protected readonly commandState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Player the removal dialog is asking about, or `null` when it is closed.
   */
  protected readonly playerPendingRemoval = signal<AdminPlayer | null>(null);

  /**
   * Whether a command is currently running, which locks the form and the dialog.
   */
  protected readonly busy = signal(false);

  /**
   * Already-translated body of the removal dialog, which states which of the two outcomes the
   * confirmation will produce.
   */
  protected readonly removalBody = computed(() => {
    const player = this.playerPendingRemoval();

    if (player === null) {
      return '';
    }

    return this.translation.translate(
      player.hasCampaignContribution
        ? 'admin.players.remove.archiveBody'
        : 'admin.players.remove.deleteBody',
    );
  });

  /**
   * Formats a player's last successful synchronization.
   *
   * Tests the value for emptiness rather than for `null`: the backend leaves null properties out
   * of its payloads entirely, so a player that was never synchronized arrives with the field
   * missing, and a `=== null` check would hand `undefined` to the formatter and render `NaN`.
   *
   * @param instant - The ISO-8601 instant, absent when never synchronized.
   * @returns The formatted timestamp, or the translated "never" label.
   */
  protected formatLastSync(instant: string | null): string {
    return instant
      ? formatSynchronizationTimestamp(instant)
      : this.translation.translate('admin.players.neverSynchronized');
  }

  /**
   * Opens the panel on a blank player.
   */
  protected startCreating(): void {
    this.editedPlayer.set(null);
    this.formOpen.set(true);
  }

  /**
   * Opens the panel on an existing player.
   *
   * @param player - The player to edit.
   */
  protected startEditing(player: AdminPlayer): void {
    this.editedPlayer.set(player);
    this.formOpen.set(true);
  }

  /**
   * Closes the panel without applying anything.
   */
  protected cancelForm(): void {
    this.formOpen.set(false);
    this.editedPlayer.set(null);
  }

  /**
   * Creates or updates the player the panel submitted.
   *
   * The display name and portrait are not exposed there, so they are carried over unchanged from
   * the edited player, or defaulted to the Riot name and `null` on creation.
   *
   * @param result - The identity the panel submitted.
   */
  protected async savePlayer(result: PlayerFormResult): Promise<void> {
    if (this.busy()) {
      return;
    }

    const edited = this.editedPlayer();

    await this.run(
      edited === null ? 'admin.players.created' : 'admin.players.updated',
      async () => {
        if (edited === null) {
          await this.adminApi.createPlayer({
            gameName: result.gameName,
            tagLine: result.tagLine,
            displayName: result.gameName,
            portrait: null,
            status: result.status,
          });
        } else {
          await this.adminApi.updatePlayer(edited.id, {
            gameName: result.gameName,
            tagLine: result.tagLine,
            displayName: edited.displayName,
            portrait: edited.portrait,
          });
        }

        this.cancelForm();
      },
    );
  }

  /**
   * Moves a player to another lifecycle status, which is also how an archived one is restored.
   *
   * @param player - The player to move.
   * @param status - The status to apply.
   */
  protected async changeStatus(player: AdminPlayer, status: AdminPlayerStatus): Promise<void> {
    await this.run('admin.players.statusChanged', () =>
      this.adminApi.changePlayerStatus(player.id, status).then(() => undefined),
    );
  }

  /**
   * Opens the removal dialog for a player.
   *
   * @param player - The player to remove.
   */
  protected askForRemoval(player: AdminPlayer): void {
    this.playerPendingRemoval.set(player);
  }

  /**
   * Closes the removal dialog without removing anything.
   */
  protected dismissRemoval(): void {
    this.playerPendingRemoval.set(null);
  }

  /**
   * Removes the player the dialog is asking about, and reports what actually happened to it.
   */
  protected async confirmRemoval(): Promise<void> {
    const player = this.playerPendingRemoval();

    if (player === null || this.busy()) {
      return;
    }

    this.busy.set(true);
    this.commandState.set({ status: 'running', message: '' });

    try {
      const result = await this.adminApi.removePlayer(player.id);

      this.commandState.set({
        status: 'done',
        message: this.translation.translate(
          result.outcome === 'ARCHIVED' ? 'admin.players.archived' : 'admin.players.deleted',
        ),
      });
      this.playerPendingRemoval.set(null);
    } catch (error: unknown) {
      this.commandState.set({
        status: 'error',
        message: resolveAdminErrorMessage(error, this.translation.translate('admin.actionFailed')),
      });
    } finally {
      this.busy.set(false);
    }
  }

  /**
   * Runs one roster command and reports its outcome above the table.
   *
   * @param successKey - Translation key of the success message.
   * @param command - The command to run.
   */
  private async run(successKey: string, command: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.commandState.set({ status: 'running', message: '' });

    try {
      await command();
      this.commandState.set({
        status: 'done',
        message: this.translation.translate(successKey),
      });
    } catch (error: unknown) {
      this.commandState.set({
        status: 'error',
        message: resolveAdminErrorMessage(error, this.translation.translate('admin.actionFailed')),
      });
    } finally {
      this.busy.set(false);
    }
  }
}
