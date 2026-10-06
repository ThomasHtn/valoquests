import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  LucideChevronDown,
  LucidePlus,
  LucidePower,
  LucideRotateCcw,
  LucideSquarePen,
  LucideTrash2,
  LucideTriangleAlert,
} from '@lucide/angular';

import { AdminApi } from '@core/admin/admin-api';
import { IDLE_ACTION } from '@core/admin/commands/admin-action.constants';
import { AdminActionState } from '@core/admin/commands/admin-action.model';
import { AdminCommandRunner } from '@core/admin/commands/admin-command-runner';
import { AdminPlayer, AdminPlayerStatus } from '@core/admin/players/admin-player.model';
import { CampaignApi } from '@core/campaign/campaign-api';
import { formatCampaignDateTime } from '@core/date/date-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge } from '@shared/status-badge/status-badge';
import { StatusBadgeTone } from '@shared/status-badge/status-badge.model';

import { GUIDE_STEPS } from './admin-players.constants';
import { PlayerFormPanel } from './player-form-panel/player-form-panel';
import { PlayerFormResult } from './player-form-panel/player-form-panel.model';

/**
 * Backoffice roster: add, edit, (de)activate and remove players.
 * Removal archives a player who took part in the campaign, so finalized weeks stay readable.
 */
@Component({
  selector: 'app-admin-players',
  imports: [
    TranslatePipe,
    RouterLink,
    Button,
    ConfirmDialog,
    PlayerFormPanel,
    ResourceState,
    SectionLabel,
    LucideChevronDown,
    LucidePlus,
    LucidePower,
    LucideRotateCcw,
    LucideSquarePen,
    LucideTrash2,
    LucideTriangleAlert,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-players.html',
  styleUrl: './admin-players.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminPlayers {
  /**
   * Campaign, read for the roster size the live run froze.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Backoffice API, for the roster and its commands.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Translation, for notices, timestamps and feedback messages.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs each roster command, tracking its state and reporting the outcome.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Every player, archived ones included.
   */
  protected readonly playersResource = this.adminApi.players;

  /**
   * Getting-started steps, numbered in this order.
   */
  protected readonly guideSteps = GUIDE_STEPS;

  /**
   * Placeholder widths of the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Players in backend order.
   */
  protected readonly players = computed(() => resourceValue(this.playersResource, []));

  /**
   * Frozen roster notice of the live run, empty otherwise (the page must work without it).
   */
  protected readonly frozenRosterLabel = computed<string>(() => {
    const campaign = resourceValue(this.campaignApi.campaign, null);
    if (!campaign || campaign.status === null || campaign.status === 'CLOSED') {
      return '';
    }

    return this.translation.translate('admin.players.frozenRoster', {
      roster: campaign.rosterSize ?? 0,
      run: campaign.number ?? 0,
    });
  });

  /**
   * Edited player, `null` when adding.
   */
  protected readonly editedPlayer = signal<AdminPlayer | null>(null);

  /**
   * Whether the form panel is open.
   */
  protected readonly formOpen = signal(false);

  /**
   * State of the last roster command.
   */
  private readonly commandState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Player the removal dialog is about, `null` while closed.
   */
  protected readonly playerPendingRemoval = signal<AdminPlayer | null>(null);

  /**
   * Whether a command is running, which locks the form and dialog.
   */
  protected readonly busy = signal(false);

  /**
   * Removal dialog body, stating whether the player is archived or deleted.
   */
  protected readonly removalBody = computed(() => {
    const player = this.playerPendingRemoval();

    if (player === null) {
      return '';
    }

    return this.translation.translate(
      player.wasOnAnyRoster
        ? 'admin.players.remove.archiveBody'
        : 'admin.players.remove.deleteBody',
    );
  });

  /**
   * Badge tone of a roster status.
   */
  protected statusTone(status: AdminPlayerStatus): StatusBadgeTone {
    return status === 'ACTIVE' ? 'brand' : status === 'INACTIVE' ? 'neutral' : 'danger';
  }

  /**
   * Label key of the (de)activation button, which flips the current status.
   */
  protected toggleActiveLabelKey(status: AdminPlayerStatus): string {
    return status === 'ACTIVE' ? 'admin.players.deactivate' : 'admin.players.activate';
  }

  /**
   * Last successful sync, or "never"; tested for emptiness since the backend omits null fields.
   */
  protected formatLastSync(instant: string | null): string {
    return instant
      ? formatCampaignDateTime(instant, this.translation.language())
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
   * Opens the panel on `player`.
   */
  protected startEditing(player: AdminPlayer): void {
    this.editedPlayer.set(player);
    this.formOpen.set(true);
  }

  /**
   * Opens the panel from a row click, unless a command runs.
   */
  protected openEditor(player: AdminPlayer): void {
    if (!this.busy()) {
      this.startEditing(player);
    }
  }

  /**
   * Opens the panel from the focused row; keys pressed on its nested buttons are theirs.
   */
  protected onRowKeydown(event: Event, player: AdminPlayer): void {
    if (event.target !== event.currentTarget) {
      return;
    }

    event.preventDefault();
    this.openEditor(player);
  }

  /**
   * Closes the panel without applying anything.
   */
  protected cancelForm(): void {
    this.formOpen.set(false);
    this.editedPlayer.set(null);
  }

  /**
   * Creates or updates the player; the display name is kept, or defaults to the Riot name.
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
   * Changes a player's status, which also restores an archived one.
   */
  protected async changeStatus(player: AdminPlayer, status: AdminPlayerStatus): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.changePlayerStatus(player.id, status), {
      state: this.commandState,
      busy: this.busy,
      successMessage: () => this.translation.translate('admin.players.statusChanged'),
    });
  }

  /**
   * Deactivates an active player, activates an inactive one.
   */
  protected async toggleActive(player: AdminPlayer): Promise<void> {
    await this.changeStatus(player, player.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
  }

  /**
   * Opens the removal dialog for `player`.
   */
  protected askForRemoval(player: AdminPlayer): void {
    this.playerPendingRemoval.set(player);
  }

  /**
   * Closes the removal dialog without removing.
   */
  protected dismissRemoval(): void {
    this.playerPendingRemoval.set(null);
  }

  /**
   * Removes the pending player and reports whether it was archived or deleted.
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
