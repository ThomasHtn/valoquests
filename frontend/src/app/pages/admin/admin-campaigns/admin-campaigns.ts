import { Component, computed, inject, signal } from '@angular/core';
import { LucideRefreshCw, LucideTrash2 } from '@lucide/angular';
import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import { CampaignApi } from '@core/campaign/campaign-api';
import {
  CAMPAIGN_DIFFICULTIES,
  CAMPAIGN_START_WEEKS,
  CAMPAIGN_WEEK_COUNT,
  CampaignDifficulty,
  CampaignStartWeek,
  CampaignStatus,
} from '@core/campaign/campaign.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { daysBetween } from '@core/date/date-time.utils';
import { addDays, formatDateRange, formatDayMonth } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { PageHeader } from '@layout/page-header/page-header';
import { Button } from '@shared/button/button';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge } from '@shared/status-badge/status-badge';
import { StatusBadgeTone } from '@shared/status-badge/status-badge.model';
import { AdminActionCard } from '../admin-action-card/admin-action-card';
import { CAMPAIGN_DAYS } from './admin-campaigns.constants';
import { LiveCampaign } from './admin-campaigns.model';

/**
 * Backoffice campaign lifecycle screen.
 *
 * The one place a campaign is opened, and the only moment its difficulty is decided: the choice is
 * frozen for the whole run and sizes the guardians, the groups of wounded and the challenge
 * rewards. Then two commands on the live campaign: stop it, or delete it. A campaign opened by
 * mistake before its first Monday is deleted rather than stopped, since stopping it would leave an
 * empty campaign in the history.
 */
@Component({
  selector: 'app-admin-campaigns',
  imports: [
    TranslatePipe,
    AdminActionCard,
    Button,
    ConfirmDialog,
    InlineMessage,
    LucideRefreshCw,
    LucideTrash2,
    PageHeader,
    ResourceState,
    SectionLabel,
    StatusBadge,
  ],
  templateUrl: './admin-campaigns.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminCampaigns {
  private readonly adminApi = inject(AdminApi);

  private readonly campaignApi = inject(CampaignApi);

  private readonly translation = inject(Translation);

  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Difficulty the next opening plays at.
   */
  protected readonly difficulty = signal<CampaignDifficulty>('AMATEUR');

  /**
   * The two difficulties, in ladder order, for the selector.
   */
  protected readonly difficulties = CAMPAIGN_DIFFICULTIES;

  /**
   * Monday the next opening starts on.
   *
   * Defaults to the next week: opening on the current one is retroactive, and an operator who did
   * not choose must not start a campaign on days that are already played.
   */
  protected readonly startWeek = signal<CampaignStartWeek>('NEXT_WEEK');

  /**
   * The two start weeks, for the selector.
   */
  protected readonly startWeeks = CAMPAIGN_START_WEEKS;

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly historyResource = this.campaignApi.history;

  /**
   * The campaign opened or running, resolved into the figures an operator decides against, or
   * `null` between two campaigns.
   */
  protected readonly live = computed<LiveCampaign | null>(() => {
    const campaign = resourceValue(this.campaignResource, null);
    if (
      !campaign ||
      campaign.id === null ||
      campaign.status === null ||
      campaign.status === 'CLOSED' ||
      campaign.firstWeekStart === null ||
      campaign.lastWeekStart === null
    ) {
      return null;
    }

    // Clamped at both ends: before the first Monday the campaign is on its zeroth day, and past
    // its last Sunday it is on its last one, never beyond.
    const dayIndex = Math.min(
      CAMPAIGN_DAYS,
      Math.max(0, daysBetween(campaign.firstWeekStart, campaign.today) + 1),
    );

    return {
      id: campaign.id,
      number: campaign.number ?? 0,
      status: campaign.status,
      difficulty: campaign.difficulty ?? 'AMATEUR',
      reference: campaign.reference ?? 0,
      rosterSize: campaign.rosterSize ?? 0,
      range: formatDateRange(campaign.firstWeekStart, addDays(campaign.lastWeekStart, 6)),
      startsOn: formatDayMonth(campaign.firstWeekStart),
      weekIndex: campaign.currentWeekIndex ?? 0,
      dayIndex,
      daysLeft: CAMPAIGN_DAYS - dayIndex,
    };
  });

  protected readonly closed = computed(() => resourceValue(this.historyResource, []));

  protected readonly openState = signal<AdminActionState>(IDLE_ACTION);

  protected readonly stopState = signal<AdminActionState>(IDLE_ACTION);

  protected readonly deleteState = signal<AdminActionState>(IDLE_ACTION);

  protected readonly openDialogOpen = signal(false);

  protected readonly stopDialogOpen = signal(false);

  /**
   * The campaign the delete dialog is asking about, or `null` while it is closed.
   */
  protected readonly pendingDeletion = signal<{
    id: number;
    number: number;
    opened: boolean;
  } | null>(null);

  protected readonly deleting = signal(false);

  /**
   * What the delete dialog says: an opened campaign has nothing but a roster and ten guardians to
   * lose, a started one has its days and its weeks.
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

  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  protected readonly campaignDays = CAMPAIGN_DAYS;

  protected readonly formatDayMonth = formatDayMonth;

  protected statusTone(status: CampaignStatus): StatusBadgeTone {
    return status === 'RUNNING' ? 'brand' : 'neutral';
  }

  protected amount(value: number): string {
    return formatDamage(value, this.translation.language());
  }

  /**
   * Formats a closed campaign's span, its last Sunday included.
   */
  protected range(firstWeekStart: string, lastWeekStart: string): string {
    return formatDateRange(firstWeekStart, addDays(lastWeekStart, 6));
  }

  protected askToOpen(): void {
    this.openDialogOpen.set(true);
  }

  protected dismissOpen(): void {
    this.openDialogOpen.set(false);
  }

  protected chooseDifficulty(difficulty: CampaignDifficulty): void {
    this.difficulty.set(difficulty);
  }

  protected chooseStartWeek(startWeek: CampaignStartWeek): void {
    this.startWeek.set(startWeek);
  }

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

  protected askToStop(): void {
    this.stopDialogOpen.set(true);
  }

  protected dismissStop(): void {
    this.stopDialogOpen.set(false);
  }

  protected async confirmStop(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.stopCampaign(), {
      state: this.stopState,
      successMessage: () => this.translation.translate('admin.campaigns.stop.done'),
      onSuccess: () => this.stopDialogOpen.set(false),
    });
  }

  protected askForDeletion(id: number, number: number, opened: boolean): void {
    this.pendingDeletion.set({ id, number, opened });
  }

  protected dismissDeletion(): void {
    this.pendingDeletion.set(null);
  }

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
