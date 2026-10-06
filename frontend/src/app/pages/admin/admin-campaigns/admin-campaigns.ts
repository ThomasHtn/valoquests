import { Component, computed, inject, signal } from '@angular/core';

import { LucideTrash2 } from '@lucide/angular';

import { AdminApi } from '@core/admin/admin-api';
import { IDLE_ACTION } from '@core/admin/commands/admin-action.constants';
import { AdminActionState } from '@core/admin/commands/admin-action.model';
import { AdminCommandRunner } from '@core/admin/commands/admin-command-runner';
import {
  CAMPAIGN_DIFFICULTIES,
  CAMPAIGN_START_WEEKS,
  CAMPAIGN_WEEK_COUNT,
} from '@core/campaign/campaign.constants';
import {
  CampaignDifficulty,
  CampaignStartWeek,
  CampaignStatus,
} from '@core/campaign/campaign.model';
import { CampaignApi } from '@core/campaign/campaign-api';
import { formatDayMonth } from '@core/date/date-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge } from '@shared/status-badge/status-badge';
import { StatusBadgeTone } from '@shared/status-badge/status-badge.model';

import { AdminActionCard } from '../admin-action-card/admin-action-card';
import { CAMPAIGN_DAYS } from './admin-campaigns.constants';
import { LiveCampaign, PendingCampaignDeletion } from './admin-campaigns.model';
import { buildLiveCampaign, formatCampaignRange } from './admin-campaigns.utils';

/**
 * Backoffice campaign lifecycle: open (difficulty frozen for the run), stop or delete.
 */
@Component({
  selector: 'app-admin-campaigns',
  imports: [
    TranslatePipe,
    AdminActionCard,
    Button,
    ConfirmDialog,
    InlineMessage,
    LucideTrash2,
    PageHeader,
    ResourceState,
    SectionLabel,
    StatusBadge,
  ],
  templateUrl: './admin-campaigns.html',
  styleUrl: './admin-campaigns.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminCampaigns {
  /**
   * Backoffice API, to open, stop and delete campaigns.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Campaign API, read for the live campaign and the closed ones.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Translation, for the feedback messages and the formatted amounts.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs each command, tracking its state and reporting the outcome.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Difficulty of the next opening.
   */
  protected readonly difficulty = signal<CampaignDifficulty>('AMATEUR');

  /**
   * Difficulties in ladder order.
   */
  protected readonly difficulties = CAMPAIGN_DIFFICULTIES;

  /**
   * Start week of the next opening, next week by default since the current one is retroactive.
   */
  protected readonly startWeek = signal<CampaignStartWeek>('NEXT_WEEK');

  /**
   * Start week options.
   */
  protected readonly startWeeks = CAMPAIGN_START_WEEKS;

  /**
   * Current campaign, the source of the live block.
   */
  protected readonly campaignResource = this.campaignApi.campaign;

  /**
   * Closed campaigns, listed under the live one.
   */
  protected readonly historyResource = this.campaignApi.history;

  /**
   * Opened or running campaign, `null` between campaigns.
   */
  protected readonly live = computed<LiveCampaign | null>(() =>
    buildLiveCampaign(resourceValue(this.campaignResource, null)),
  );

  /**
   * Closed campaigns, empty while the history loads or fails.
   */
  protected readonly closed = computed(() => resourceValue(this.historyResource, []));

  /**
   * State of the open command.
   */
  protected readonly openState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the stop command.
   */
  protected readonly stopState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the delete command.
   */
  private readonly deleteState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the open command runs, which locks its dialog.
   */
  protected readonly opening = computed(() => this.openState().status === 'running');

  /**
   * Whether the stop command runs, which locks its button and dialog.
   */
  protected readonly stopping = computed(() => this.stopState().status === 'running');

  /**
   * Whether the open confirmation dialog is shown.
   */
  protected readonly openDialogOpen = signal(false);

  /**
   * Whether the stop confirmation dialog is shown.
   */
  protected readonly stopDialogOpen = signal(false);

  /**
   * Campaign the delete dialog is about, `null` while closed.
   */
  protected readonly pendingDeletion = signal<PendingCampaignDeletion | null>(null);

  /**
   * Whether a deletion runs, so a second confirm is ignored.
   */
  protected readonly deleting = signal(false);

  /**
   * Delete dialog body, which differs for a campaign not started yet.
   */
  protected readonly deletionBody = computed(() => {
    const pending = this.pendingDeletion();
    if (pending === null) {
      return '';
    }
    return this.translation.translate(
      pending.opened
        ? 'admin.campaigns.delete.confirmBodyOpened'
        : 'admin.campaigns.delete.confirmBody',
      { number: pending.number },
    );
  });

  /**
   * Weeks in a campaign, the denominator of the week and guardian counters.
   */
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  /**
   * Days in a campaign, the denominator of the day counter.
   */
  protected readonly campaignDays = CAMPAIGN_DAYS;

  /**
   * Date formatter, exposed to the template for the stop date.
   */
  protected readonly formatDayMonth = formatDayMonth;

  /**
   * Badge tone, highlighting a running campaign over an opened one.
   */
  protected statusTone(status: CampaignStatus): StatusBadgeTone {
    return status === 'RUNNING' ? 'brand' : 'neutral';
  }

  /**
   * Formats a reference or a population in the current language.
   */
  protected amount(value: number): string {
    return formatFigure(value, this.translation.language());
  }

  /**
   * Closed campaign's span, its last Sunday included.
   */
  protected range(firstWeekStart: string, lastWeekStart: string): string {
    return formatCampaignRange(firstWeekStart, lastWeekStart);
  }

  /**
   * Shows the open confirmation dialog.
   */
  protected askToOpen(): void {
    this.openDialogOpen.set(true);
  }

  /**
   * Hides the open confirmation dialog without opening anything.
   */
  protected dismissOpen(): void {
    this.openDialogOpen.set(false);
  }

  /**
   * Records the difficulty picked for the next opening.
   */
  protected chooseDifficulty(difficulty: CampaignDifficulty): void {
    this.difficulty.set(difficulty);
  }

  /**
   * Records the start week picked for the next opening.
   */
  protected chooseStartWeek(startWeek: CampaignStartWeek): void {
    this.startWeek.set(startWeek);
  }

  /**
   * Opens a campaign with the picked difficulty and start week, then closes the dialog.
   */
  protected async confirmOpen(): Promise<void> {
    await this.commandRunner.run(
      () => this.adminApi.openCampaign(this.difficulty(), this.startWeek()),
      {
        state: this.openState,
        successMessage: (campaign) =>
          this.translation.translate('admin.campaigns.open.done', {
            number: campaign.number,
            date: formatDayMonth(campaign.firstWeekStart),
          }),
        onSuccess: () => this.openDialogOpen.set(false),
      },
    );
  }

  /**
   * Shows the stop confirmation dialog.
   */
  protected askToStop(): void {
    this.stopDialogOpen.set(true);
  }

  /**
   * Hides the stop confirmation dialog without stopping anything.
   */
  protected dismissStop(): void {
    this.stopDialogOpen.set(false);
  }

  /**
   * Stops the running campaign, then closes the dialog.
   */
  protected async confirmStop(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.stopCampaign(), {
      state: this.stopState,
      successMessage: () => this.translation.translate('admin.campaigns.stop.done'),
      onSuccess: () => this.stopDialogOpen.set(false),
    });
  }

  /**
   * Opens the delete dialog for one campaign.
   */
  protected askForDeletion(id: number, number: number, opened: boolean): void {
    this.pendingDeletion.set({ id, number, opened });
  }

  /**
   * Closes the delete dialog without deleting anything.
   */
  protected dismissDeletion(): void {
    this.pendingDeletion.set(null);
  }

  /**
   * Deletes the pending campaign, once even if confirmed twice.
   */
  protected async confirmDeletion(): Promise<void> {
    const pending = this.pendingDeletion();
    if (pending === null || this.deleting()) {
      return;
    }

    await this.commandRunner.run(() => this.adminApi.deleteCampaign(pending.id), {
      state: this.deleteState,
      busy: this.deleting,
      successMessage: () => this.translation.translate('admin.campaigns.delete.done'),
      onSuccess: () => this.pendingDeletion.set(null),
    });
  }
}
