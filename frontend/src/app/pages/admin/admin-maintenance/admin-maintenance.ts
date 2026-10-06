import { Component, computed, inject, signal } from '@angular/core';

import { AdminApi } from '@core/admin/admin-api';
import { IDLE_ACTION } from '@core/admin/commands/admin-action.constants';
import { AdminActionState } from '@core/admin/commands/admin-action.model';
import { AdminCommandRunner } from '@core/admin/commands/admin-command-runner';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { SectionLabel } from '@shared/section-label/section-label';

import { RESET_DATA_GROUPS } from './admin-maintenance.constants';

/**
 * Campaign reset, alone on its page behind a typed confirmation so it is never hit by habit.
 */
@Component({
  selector: 'app-admin-maintenance',
  imports: [TranslatePipe, Button, ConfirmDialog, InlineMessage, PageHeader, SectionLabel],
  templateUrl: './admin-maintenance.html',
  styleUrl: './admin-maintenance.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminMaintenance {
  /**
   * Backoffice API, for the reset and the latest synchronization.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Translation, for the confirmation phrase and the success message.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs the reset, tracking its state and reporting the outcome.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Records the reset clears, then those it keeps.
   */
  protected readonly dataGroups = RESET_DATA_GROUPS;

  /**
   * State of the reset command.
   */
  protected readonly resetState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the confirmation dialog is open.
   */
  protected readonly dialogOpen = signal(false);

  /**
   * Whether the reset is running.
   */
  protected readonly busy = signal(false);

  /**
   * Whether a synchronization is in flight, shown upfront rather than left to the 409.
   */
  protected readonly synchronizing = this.adminApi.synchronizing;

  /**
   * Translated phrase the operator must type to confirm.
   */
  protected readonly confirmationPhrase = computed(() =>
    this.translation.translate('admin.maintenance.reset.phrase'),
  );

  /**
   * Refreshes the synchronization status, which may date from an earlier page.
   */
  constructor() {
    this.adminApi.latestSynchronization.reload();
  }

  /**
   * Opens the confirmation dialog.
   */
  protected askForReset(): void {
    this.dialogOpen.set(true);
  }

  /**
   * Closes the confirmation dialog without resetting.
   */
  protected dismissReset(): void {
    this.dialogOpen.set(false);
  }

  /**
   * Clears every record derived from match history.
   */
  protected async confirmReset(): Promise<void> {
    if (this.busy()) {
      return;
    }

    // The dialog stays open on failure.
    await this.commandRunner.run(() => this.adminApi.resetCampaign(), {
      state: this.resetState,
      busy: this.busy,
      successMessage: () => this.translation.translate('admin.maintenance.reset.done'),
      onSuccess: () => this.dialogOpen.set(false),
    });
  }
}
