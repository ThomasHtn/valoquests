import { Component, computed, inject, signal } from '@angular/core';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import { CampaignRun } from '@core/admin/admin.model';
import { formatDateRange } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { PageHeader } from '@layout/page-header/page-header';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge, StatusBadgeTone } from '@shared/status-badge/status-badge';
import { AdminActionCard } from '../admin-action-card/admin-action-card';

/**
 * Backoffice campaign lifecycle screen.
 *
 * Lists every run of the campaign — the one in progress first, then every closed one — and carries
 * the three operations design-review.md's lot 1 asks for: starting a campaign, stopping the one in
 * progress (its score frozen at today rather than at its natural settlement day), and switching
 * whether the weekly rollover may open the next run on its own once this one closes.
 *
 * That last setting only ever gates the *scheduled* rollover opening a brand new run once none is
 * open at all — a campaign already under way always runs its own ten weeks regardless of it, and a
 * page read still opens one lazily as the safety net it has always been. Turning automatic renewal
 * off is therefore a promise about tonight's tick, not a guarantee the site shows "no campaign"
 * indefinitely the moment someone loads a page.
 */
@Component({
  selector: 'app-admin-campaigns',
  imports: [
    TranslatePipe,
    AdminActionCard,
    ConfirmDialog,
    PageHeader,
    ResourceState,
    SectionLabel,
    StatusBadge,
  ],
  templateUrl: './admin-campaigns.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminCampaigns {
  /**
   * Data-access service backing every command and the campaigns resource.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * i18n service used to resolve outcome messages and status labels.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs each command below and reports its running/done/error outcome in the card that
   * triggered it.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Resource holding the campaign's lifecycle.
   */
  protected readonly campaignsResource = this.adminApi.campaigns;

  /**
   * Every run, current first, or `[]` while loading.
   */
  protected readonly runs = computed<readonly CampaignRun[]>(
    () => resourceValue(this.campaignsResource, undefined)?.runs ?? [],
  );

  /**
   * Whether a campaign is currently running — what disables "start" and enables "stop".
   */
  protected readonly hasRunningCampaign = computed(() =>
    this.runs().some((run) => run.status === 'RUNNING'),
  );

  /**
   * Whether the weekly rollover may open a new run on its own, or `false` while loading.
   */
  protected readonly autoRenewEnabled = computed(
    () => resourceValue(this.campaignsResource, undefined)?.autoRenewEnabled ?? false,
  );

  /**
   * State of the start-campaign command.
   */
  protected readonly startState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the stop-campaign command.
   */
  protected readonly stopState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the automatic-renewal toggle.
   */
  protected readonly autoRenewState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the stop confirmation dialog is open.
   */
  protected readonly stopDialogOpen = signal(false);

  /**
   * Resolves the status badge tone for a run, exposed to the template.
   *
   * @param status - The run's own place in the lifecycle.
   * @returns The tone to render the badge in.
   */
  protected statusTone(status: CampaignRun['status']): StatusBadgeTone {
    return status === 'RUNNING' ? 'brand' : 'neutral';
  }

  /**
   * Formats a run's span as `DD/MM - DD/MM`, exposed to the template.
   *
   * @param run - The run to format.
   * @returns The formatted date range.
   */
  protected dateRangeLabel(run: CampaignRun): string {
    return formatDateRange(run.firstDay, run.finalDay);
  }

  /**
   * Opens the stop confirmation dialog.
   */
  protected askToStop(): void {
    this.stopDialogOpen.set(true);
  }

  /**
   * Closes the stop confirmation dialog without stopping anything.
   */
  protected dismissStop(): void {
    this.stopDialogOpen.set(false);
  }

  /**
   * Stops the campaign in progress, once confirmed.
   */
  protected async confirmStop(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.stopCampaign(), {
      state: this.stopState,
      successMessage: () => this.translation.translate('admin.campaigns.stop.done'),
      onSuccess: () => this.stopDialogOpen.set(false),
    });
  }

  /**
   * Starts a new campaign today.
   */
  protected async startCampaign(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.startCampaign(), {
      state: this.startState,
      successMessage: () => this.translation.translate('admin.campaigns.start.done'),
    });
  }

  /**
   * Switches automatic renewal to the opposite of its current value.
   */
  protected async toggleAutoRenew(): Promise<void> {
    const next = !this.autoRenewEnabled();

    await this.commandRunner.run(() => this.adminApi.setCampaignAutoRenew(next), {
      state: this.autoRenewState,
      successMessage: () =>
        this.translation.translate(
          next ? 'admin.campaigns.autoRenew.enabled' : 'admin.campaigns.autoRenew.disabled',
        ),
    });
  }
}
