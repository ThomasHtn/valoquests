import { Component, computed, inject, signal } from '@angular/core';

import { AdminApi } from '@core/admin/admin-api';
import { resolveAdminErrorMessage } from '@core/admin/admin-error.utils';
import { IN_FLIGHT_SYNCHRONIZATION_STATUSES } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { AdminActionState, IDLE_ACTION } from '../admin-action.model';

/**
 * Translation keys of the data the campaign reset clears, listed for the operator before they
 * confirm.
 *
 * Spelled out rather than summarised as "everything": the reset keeps the roster and the
 * catalogues, and an operator who assumed otherwise would hesitate over an action that is in fact
 * safe for the players they set up.
 */
const CLEARED_DATA_KEYS: readonly string[] = [
  'admin.maintenance.reset.cleared.matches',
  'admin.maintenance.reset.cleared.challenges',
  'admin.maintenance.reset.cleared.rankings',
  'admin.maintenance.reset.cleared.bosses',
  'admin.maintenance.reset.cleared.synchronizations',
];

/**
 * Translation keys of what the campaign reset leaves untouched.
 */
const KEPT_DATA_KEYS: readonly string[] = [
  'admin.maintenance.reset.kept.players',
  'admin.maintenance.reset.kept.challengeCatalogue',
  'admin.maintenance.reset.kept.bossCatalogue',
];

/**
 * Backoffice maintenance screen.
 *
 * Holds the one operation that destroys data the API cannot give back: clearing every record
 * derived from match history so a new campaign starts from an empty base. It is deliberately alone
 * on its own page, behind a typed confirmation, rather than sitting among the repair commands where
 * it could be reached by habit.
 */
@Component({
  selector: 'app-admin-maintenance',
  imports: [TranslatePipe, ConfirmDialog, SectionDivider],
  templateUrl: './admin-maintenance.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminMaintenance {
  /**
   * Data-access service backing the reset command and the synchronization resource.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * i18n service used to resolve the confirmation phrase and the outcome messages.
   */
  private readonly translation = inject(Translation);

  /**
   * Translation keys of the data the reset clears.
   */
  protected readonly clearedDataKeys = CLEARED_DATA_KEYS;

  /**
   * Translation keys of the data the reset keeps.
   */
  protected readonly keptDataKeys = KEPT_DATA_KEYS;

  /**
   * State of the reset command.
   */
  protected readonly resetState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the confirmation dialog is on screen.
   */
  protected readonly dialogOpen = signal(false);

  /**
   * Whether the reset is currently running.
   */
  protected readonly busy = signal(false);

  /**
   * Whether a synchronization is in flight, which the backend refuses to reset over.
   *
   * Reported here rather than left to the 409: the operator would otherwise read the refusal as the
   * reset being broken instead of as something being in the way.
   */
  protected readonly synchronizing = computed(() => {
    const execution = resourceValue(this.adminApi.latestSynchronization, undefined);

    return execution !== undefined && IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(execution.status);
  });

  /**
   * Phrase the operator must type to confirm the reset.
   *
   * Translated, so it stays a word the operator is actually reading rather than one they copy
   * without understanding.
   */
  protected readonly confirmationPhrase = computed(() =>
    this.translation.translate('admin.maintenance.reset.phrase'),
  );

  /**
   * Opens the confirmation dialog.
   */
  protected askForReset(): void {
    this.dialogOpen.set(true);
  }

  /**
   * Closes the confirmation dialog without resetting anything.
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

    this.busy.set(true);
    this.resetState.set({ status: 'running', message: '' });

    try {
      await this.adminApi.resetCampaign();
      this.resetState.set({
        status: 'done',
        message: this.translation.translate('admin.maintenance.reset.done'),
      });
      this.dialogOpen.set(false);
    } catch (error: unknown) {
      // The dialog stays open on failure: nothing was cleared, so the operator is still deciding.
      this.resetState.set({
        status: 'error',
        message: resolveAdminErrorMessage(error, this.translation.translate('admin.actionFailed')),
      });
    } finally {
      this.busy.set(false);
    }
  }
}
