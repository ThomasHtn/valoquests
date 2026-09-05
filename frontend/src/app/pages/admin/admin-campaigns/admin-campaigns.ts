import { Component, computed, inject, signal } from '@angular/core';
import { LucideRefreshCw, LucideTrash2 } from '@lucide/angular';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import { IN_FLIGHT_SYNCHRONIZATION_STATUSES, PlayerCalibration } from '@core/admin/admin.model';
import { CampaignApi } from '@core/campaign/campaign-api';
import { CAMPAIGN_WEEK_COUNT, CampaignStatus } from '@core/campaign/campaign.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
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
import { StatusBadge, StatusBadgeTone } from '@shared/status-badge/status-badge';
import { AdminActionCard } from '../admin-action-card/admin-action-card';

/**
 * Days a campaign spans: ten weeks of seven days.
 */
const CAMPAIGN_DAYS = CAMPAIGN_WEEK_COUNT * 7;

/**
 * Whole days from one ISO date to another, ignoring clocks. On `Date.UTC` so a daylight-saving
 * change never makes a seventy-day count a day short.
 *
 * @param from - Earlier ISO date, `YYYY-MM-DD`.
 * @param to - Later ISO date, `YYYY-MM-DD`.
 * @returns Whole days between the two.
 */
function daysBetween(from: string, to: string): number {
  const asUtc = (iso: string): number => {
    const [year, month, day] = iso.split('-').map(Number);
    return Date.UTC(year, month - 1, day);
  };

  return Math.round((asUtc(to) - asUtc(from)) / 86_400_000);
}

/**
 * A campaign the operator can act on, with the figures the decision is made against.
 */
interface LiveCampaign {
  readonly id: number;
  readonly number: number;
  readonly status: CampaignStatus;
  readonly tier: string;
  readonly reference: number;
  readonly rosterSize: number;
  readonly range: string;
  readonly startsOn: string;
  readonly weekIndex: number;
  readonly dayIndex: number;
  readonly daysLeft: number;
}

/**
 * Backoffice campaign lifecycle screen.
 *
 * The one place a campaign is opened, and the only moment its calibration can be looked at before
 * it is decided for good: the page shows the measure a campaign opened today would be given,
 * operator by operator, with how far back each one's history reaches. Then the three commands:
 * import the calibration window, open, stop. A campaign opened by mistake before its first Monday
 * is deleted rather than stopped, since stopping it would leave an empty campaign in the history.
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

  protected readonly calibrationResource = this.adminApi.calibration;

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly historyResource = this.campaignApi.history;

  protected readonly calibration = computed(() =>
    resourceValue(this.calibrationResource, undefined),
  );

  /**
   * Operators whose history does not reach the start of the window and who are not beginners:
   * the ones an import would help.
   */
  protected readonly uncovered = computed<readonly PlayerCalibration[]>(
    () => this.calibration()?.players.filter((player) => !player.covered && !player.beginner) ?? [],
  );

  /**
   * Whether a synchronization or an import is in flight, which the backend refuses to run beside.
   */
  protected readonly synchronizing = computed(() => {
    const execution = resourceValue(this.adminApi.latestSynchronization, undefined);
    return execution !== undefined && IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(execution.status);
  });

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
      tier: campaign.tier ?? 'AMATEUR',
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

  protected readonly backfillState = signal<AdminActionState>(IDLE_ACTION);

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

  /**
   * Formats a day with its year: the calibration window reaches months back, and a first match
   * from last year shown as `01/06` reads as this year's.
   *
   * @param iso - ISO-8601 date, `YYYY-MM-DD`.
   * @returns The day in the reader's own notation.
   */
  protected fullDate(iso: string): string {
    return new Intl.DateTimeFormat(this.translation.language() === 'fr' ? 'fr-FR' : 'en-GB', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(`${iso}T00:00:00`));
  }

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

  /**
   * Formats the volume factor as the multiplier the challenge targets are scaled by.
   */
  protected factor(value: number): string {
    return `× ${new Intl.NumberFormat(this.translation.language() === 'fr' ? 'fr-FR' : 'en-GB', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value)}`;
  }

  protected async backfill(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.backfillHistory(), {
      state: this.backfillState,
      successMessage: () => this.translation.translate('admin.campaigns.backfill.accepted'),
    });
  }

  protected askToOpen(): void {
    this.openDialogOpen.set(true);
  }

  protected dismissOpen(): void {
    this.openDialogOpen.set(false);
  }

  protected async confirmOpen(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.openCampaign(), {
      state: this.openState,
      successMessage: (campaign) =>
        this.translation.translate('admin.campaigns.open.done', {
          number: campaign.number,
          date: formatDayMonth(campaign.firstWeekStart),
        }),
      onSuccess: () => this.openDialogOpen.set(false),
    });
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
