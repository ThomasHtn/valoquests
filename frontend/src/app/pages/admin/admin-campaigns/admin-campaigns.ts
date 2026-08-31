import { Component, computed, inject, signal } from '@angular/core';
import { LucideTrash2 } from '@lucide/angular';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import { CampaignRun } from '@core/admin/admin.model';
import { addDays, formatDateRange, formatDayMonth } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { PageHeader } from '@layout/page-header/page-header';
import { Button } from '@shared/button/button';
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
/**
 * Weeks a run spans. Mirrors the backend's own run length; the endpoint sends the run's two dates
 * rather than its week count, so the ladder this divides by is stated once here.
 */
const RUN_WEEKS = 10;

/**
 * Whole days from one ISO date to another, ignoring clocks.
 *
 * Built on `Date.UTC` rather than on local dates: a run spanning a daylight-saving change is
 * otherwise a day short or a day long, which on a seventy-one-day count is a visible error.
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
 * The Monday the weekly rollover next lands on.
 *
 * Deliberately not `nextWeekStart`, which adds a day to a week's *end*: handed today it answers
 * "tomorrow", which is the rollover date exactly one day in seven. Today counts as the next Monday
 * only if the rollover has not already run, which it has by the time anyone reads this — so a Monday
 * answers with the Monday after.
 *
 * @returns Next Monday's `YYYY-MM-DD`.
 */
function nextMondayIso(): string {
  const today = todayIso();
  const weekday = new Date(`${today}T00:00:00Z`).getUTCDay();
  // `getUTCDay` counts from Sunday; Monday is 1, and a Monday waits a full week.
  return addDays(today, (8 - weekday) % 7 || 7);
}

/**
 * Today, as the ISO date the run's own dates are expressed in.
 *
 * @returns Today's `YYYY-MM-DD`.
 */
function todayIso(): string {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
}

@Component({
  selector: 'app-admin-campaigns',
  imports: [
    TranslatePipe,
    AdminActionCard,
    Button,
    ConfirmDialog,
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
   * The campaign in progress as the endpoint sends it, or `null` when nothing is running.
   *
   * Kept beside {@link currentRun} rather than folded into it: deleting a campaign needs the run's
   * own identifier, which the display figures below deliberately do not carry.
   */
  protected readonly runningRun = computed<CampaignRun | null>(
    () => this.runs().find((candidate) => candidate.status === 'RUNNING') ?? null,
  );

  /**
   * The campaign in progress, resolved into the figures an operator needs before touching anything.
   *
   * The page reported the running campaign as one row of a table — dates, roster, score — which is
   * enough to list it and not enough to decide anything about it. Where it stands in its own ten
   * weeks, how much is left and when the next automatic rollover lands are what an operator is
   * actually looking for, and all three are arithmetic on the two dates the endpoint already sends.
   *
   * `null` when nothing is running, which is when this card would have nothing to say.
   */
  protected readonly currentRun = computed(() => {
    const run = this.runningRun();
    if (run === null) {
      return null;
    }

    const totalDays = daysBetween(run.firstDay, run.finalDay) + 1;
    // Clamped at both ends: a run opened today is on its first day, never its zeroth, and one whose
    // settlement day has passed without a rollover is on its last, never past it.
    const dayIndex = Math.min(totalDays, Math.max(1, daysBetween(run.firstDay, todayIso()) + 1));

    return {
      dayIndex,
      totalDays,
      weekIndex: Math.min(RUN_WEEKS, Math.floor((dayIndex - 1) / 7) + 1),
      totalWeeks: RUN_WEEKS,
      daysLeft: totalDays - dayIndex,
      nextRollover: formatDayMonth(nextMondayIso()),
      rangeLabel: this.dateRangeLabel(run),
      rosterSize: run.rosterSize,
      score: run.score,
    };
  });

  /**
   * Campaigns already closed, newest first — everything the card above does not already carry.
   *
   * The running one is filtered out rather than listed twice: it has a card of its own now, and a
   * row restating its dates, its roster and its score directly above that card was the same
   * campaign reported at two levels of detail on one screen.
   */
  protected readonly pastRuns = computed<readonly CampaignRun[]>(() =>
    this.runs().filter((run) => run.status !== 'RUNNING'),
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
   * State of the delete-campaign command.
   */
  protected readonly deleteState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the stop confirmation dialog is open.
   */
  protected readonly stopDialogOpen = signal(false);

  /**
   * The campaign the delete dialog is asking about, or `null` while it is closed.
   */
  protected readonly runPendingDeletion = signal<CampaignRun | null>(null);

  /**
   * Whether a deletion is in flight, which disables every delete button while it lands.
   */
  protected readonly deleting = signal(false);

  /**
   * What the delete dialog says, naming the campaign and what goes with it.
   *
   * Spelled out rather than left to a generic warning: deleting a campaign takes its colony and its
   * boss fights, and keeps the matches, the weekly challenges and the rankings — an operator who
   * assumed otherwise would hesitate over an action that leaves the weekly ranking untouched.
   */
  protected readonly deletionBody = computed(() => {
    const run = this.runPendingDeletion();

    if (run === null) {
      return '';
    }

    return this.translation.translate(
      run.status === 'RUNNING'
        ? 'admin.campaigns.delete.confirmBodyRunning'
        : 'admin.campaigns.delete.confirmBody',
      { range: this.dateRangeLabel(run) },
    );
  });

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
   * Opens the delete dialog for one campaign.
   *
   * @param run - The campaign to delete.
   */
  protected askForDeletion(run: CampaignRun): void {
    this.runPendingDeletion.set(run);
  }

  /**
   * Closes the delete dialog without deleting anything.
   */
  protected dismissDeletion(): void {
    this.runPendingDeletion.set(null);
  }

  /**
   * Deletes the campaign the dialog is asking about.
   */
  protected async confirmDeletion(): Promise<void> {
    const run = this.runPendingDeletion();

    if (run === null || this.deleting()) {
      return;
    }

    await this.commandRunner.run(() => this.adminApi.deleteCampaign(run.id), {
      state: this.deleteState,
      busy: this.deleting,
      successMessage: () => this.translation.translate('admin.campaigns.delete.done'),
      onSuccess: () => this.runPendingDeletion.set(null),
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
