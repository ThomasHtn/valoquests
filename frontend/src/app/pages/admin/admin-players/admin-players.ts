import { Component, computed, inject, signal } from '@angular/core';
import {
  LucidePlus,
  LucidePower,
  LucideRotateCcw,
  LucideSquarePen,
  LucideTrash2,
} from '@lucide/angular';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import { AdminPlayer, AdminPlayerStatus } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { formatSynchronizationTimestamp } from '@layout/sidebar/sidebar.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { PageHeader } from '@layout/page-header/page-header';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionLabel } from '@shared/section-label/section-label';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { StatusBadge, StatusBadgeTone } from '@shared/status-badge/status-badge';
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
    SectionLabel,
    LucidePlus,
    LucidePower,
    LucideRotateCcw,
    LucideSquarePen,
    LucideTrash2,
    PageHeader,
    StatusBadge,
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
   * Runs each roster command below and reports its running/done/error outcome.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

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
   * Tone `app-status-badge` renders a roster status with: the brand tint for a player still being
   * tracked, neutral for one paused, danger for one archived out of the roster.
   *
   * @param status - The status to map.
   * @returns The badge tone for that status.
   */
  protected statusTone(status: AdminPlayerStatus): StatusBadgeTone {
    return status === 'ACTIVE' ? 'brand' : status === 'INACTIVE' ? 'neutral' : 'danger';
  }

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
   * The display name is not exposed there, so it is carried over unchanged from the edited player,
   * or defaulted to the Riot name on creation.
   *
   * @param result - The identity the panel submitted.
   */
  protected async savePlayer(result: PlayerFormResult): Promise<void> {
    if (this.busy()) {
      return;
    }

    const edited = this.editedPlayer();

    await this.commandRunner.run(
      () =>
        edited === null
          ? this.adminApi.createPlayer({
              gameName: result.gameName,
              tagLine: result.tagLine,
              displayName: result.gameName,
              portrait: result.portrait,
              status: result.status,
            })
          : this.adminApi.updatePlayer(edited.id, {
              gameName: result.gameName,
              tagLine: result.tagLine,
              displayName: edited.displayName,
              portrait: result.portrait,
            }),
      {
        state: this.commandState,
        busy: this.busy,
        successMessage: () =>
          this.translation.translate(
            edited === null ? 'admin.players.created' : 'admin.players.updated',
          ),
        onSuccess: () => this.cancelForm(),
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
    await this.commandRunner.run(() => this.adminApi.changePlayerStatus(player.id, status), {
      state: this.commandState,
      busy: this.busy,
      successMessage: () => this.translation.translate('admin.players.statusChanged'),
    });
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

    await this.commandRunner.run(() => this.adminApi.removePlayer(player.id), {
      state: this.commandState,
      busy: this.busy,
      successMessage: (result) =>
        this.translation.translate(
          result.outcome === 'ARCHIVED' ? 'admin.players.archived' : 'admin.players.deleted',
        ),
      onSuccess: () => this.playerPendingRemoval.set(null),
    });
  }
}
