import { Component, computed, effect, inject, signal } from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';
import { AdminActionState } from '@core/admin/commands/admin-action.model';
import { IDLE_ACTION } from '@core/admin/commands/admin-action.constants';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/commands/admin-command-runner';
import { IN_FLIGHT_SYNCHRONIZATION_STATUSES } from '@core/admin/synchronization/admin-synchronization.constants';
import { SynchronizationRunStatus } from '@core/admin/synchronization/admin-synchronization.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { SnackbarQueue } from '@core/snackbar/snackbar';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { formatCampaignDateTime } from '@core/date/date-format.utils';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { PageHeader } from '@layout/page-header/page-header';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge } from '@shared/status-badge/status-badge';
import { StatusBadgeTone } from '@shared/status-badge/status-badge.model';
import { AdminActionCard } from '../admin-action-card/admin-action-card';
import { SYNCHRONIZATION_POLL_INTERVAL_MS } from './admin-operations.constants';
import { SynchronizationFigure } from './admin-operations.model';

/**
 * Backoffice operations: each card triggers a whole scheduled job.
 * A synchronization answers `202`, so the page polls the latest run while it is in flight.
 */
@Component({
  selector: 'app-admin-operations',
  imports: [
    TranslatePipe,
    AdminActionCard,
    ConfirmDialog,
    InlineMessage,
    LucideChevronDown,
    LucideChevronLeft,
    LucideChevronRight,
    PageHeader,
    ResourceState,
    SectionLabel,
    Select,
    StatusBadge,
  ],
  templateUrl: './admin-operations.html',
  styleUrl: './admin-operations.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminOperations {
  /**
   * Backoffice API, for the jobs and the synchronization runs.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Translation, for labels, statuses and feedback messages.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs each job, tracking its state and reporting the outcome.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Reports "no player selected", which never reaches the runner.
   */
  private readonly snackbar = inject(SnackbarQueue);

  /**
   * Tracked players, for the per-player picker.
   */
  private readonly playersResource = this.adminApi.players;

  /**
   * Latest synchronization execution.
   */
  private readonly synchronizationResource = this.adminApi.latestSynchronization;

  /**
   * Latest execution, `undefined` when none ever ran.
   */
  protected readonly synchronization = computed(() =>
    resourceValue(this.synchronizationResource, undefined),
  );

  /**
   * Whether a synchronization runs; disables both sync buttons rather than meet a 409.
   */
  protected readonly synchronizing = computed(() => {
    const execution = this.synchronization();

    return execution !== undefined && IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(execution.status);
  });

  /**
   * Player to synchronize, `null` while none is chosen.
   */
  protected readonly selectedPlayerId = signal<number | null>(null);

  /**
   * Picker options, archived players left out since the backend skips them.
   */
  protected readonly playerOptions = computed<readonly SelectOption<number | null>[]>(() => [
    { value: null, label: this.translation.translate('admin.operations.syncPlayer.placeholder') },
    ...resourceValue(this.playersResource, [])
      .filter((player) => player.status !== 'ARCHIVED')
      .map((player) => ({
        value: player.id as number | null,
        label: `${player.displayName} (${player.gameName}#${player.tagLine})`,
      })),
  ]);

  /**
   * State of the squad-wide synchronization.
   */
  protected readonly syncAllState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the per-player synchronization.
   */
  protected readonly syncPlayerState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the challenge redraw.
   */
  protected readonly redrawState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the redraw dialog is open.
   */
  protected readonly redrawDialogOpen = signal(false);

  /**
   * Whether the redraw is running, which locks the dialog.
   */
  protected readonly redrawing = signal(false);

  /**
   * State of the daily tick.
   */
  protected readonly dailyTickState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the weekly rollover.
   */
  protected readonly rolloverState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Translated status of the latest execution.
   */
  protected readonly statusLabel = computed(() => {
    const execution = this.synchronization();

    return execution === undefined
      ? this.translation.translate('admin.operations.status.none')
      : this.translation.translate(`admin.operations.status.${execution.status}`);
  });

  /**
   * Badge tone of the latest execution, brand while it runs.
   */
  protected readonly statusTone = computed<StatusBadgeTone>(() =>
    this.synchronizing() ? 'brand' : 'neutral',
  );

  /**
   * Counters of the latest execution, failures in danger tones once any occurred.
   */
  protected readonly statusFigures = computed<readonly SynchronizationFigure[]>(() => {
    const execution = this.synchronization();

    if (execution === undefined) {
      return [];
    }

    return [
      {
        labelKey: 'admin.operations.playersProcessed',
        value: execution.playersProcessed,
        alert: false,
      },
      {
        labelKey: 'admin.operations.matchesImported',
        value: execution.matchesImported,
        alert: false,
      },
      {
        labelKey: 'admin.operations.failures',
        value: execution.failureCount,
        alert: execution.failureCount > 0,
      },
    ];
  });

  /**
   * Start time of the latest execution, `''` when absent (the backend omits null fields).
   */
  protected readonly startedLabel = computed(() => {
    const startedAt = this.synchronization()?.startedAt;

    return startedAt ? formatCampaignDateTime(startedAt, this.translation.language()) : '';
  });

  /**
   * Zero-based history page on screen.
   */
  protected readonly historyPage = signal(0);

  /**
   * Requested history page.
   */
  protected readonly historyResource = this.adminApi.synchronizationHistory(this.historyPage);

  /**
   * Executions of the page, most recent first.
   */
  protected readonly historyRows = computed(
    () => resourceValue(this.historyResource, undefined)?.content ?? [],
  );

  /**
   * Whether an older page exists.
   */
  protected readonly hasNextHistoryPage = computed(() => {
    const page = resourceValue(this.historyResource, undefined);

    return page !== undefined && this.historyPage() + 1 < page.totalPages;
  });

  /**
   * Execution whose per-player results are open, `null` when none.
   */
  protected readonly expandedExecutionId = signal<number | null>(null);

  /**
   * Per-player results of {@link expandedExecutionId}, fetched on expand.
   */
  protected readonly executionDetailsResource = this.adminApi.synchronizationDetails(
    this.expandedExecutionId,
  );

  /**
   * Per-player results, `[]` while collapsed or loading.
   */
  protected readonly executionDetailsPlayers = computed(
    () => resourceValue(this.executionDetailsResource, undefined)?.players ?? [],
  );

  /**
   * Polls the running synchronization; the interval only lives while one is in flight.
   */
  constructor() {
    effect((onCleanup) => {
      if (!this.synchronizing()) {
        return;
      }

      const handle = setInterval(
        () => this.synchronizationResource.reload(),
        SYNCHRONIZATION_POLL_INTERVAL_MS,
      );

      onCleanup(() => clearInterval(handle));
    });
  }

  /**
   * Start time of a history run, a dash when the backend omitted it.
   */
  protected formatRunStart(startedAt: string | null): string {
    return startedAt ? formatCampaignDateTime(startedAt, this.translation.language()) : '—';
  }

  /**
   * Badge tone of a history status.
   */
  protected historyStatusTone(status: SynchronizationRunStatus): StatusBadgeTone {
    if (IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(status)) {
      return 'brand';
    }

    return status === 'FAILED' || status === 'PARTIAL' ? 'danger' : 'neutral';
  }

  /**
   * Toggles an execution's per-player results.
   */
  protected toggleExecution(executionId: number): void {
    this.expandedExecutionId.update((current) => (current === executionId ? null : executionId));
  }

  /**
   * Steps to the next, older page.
   */
  protected loadNextHistoryPage(): void {
    if (this.hasNextHistoryPage()) {
      this.historyPage.update((page) => page + 1);
    }
  }

  /**
   * Steps back to the previous, more recent page.
   */
  protected loadPreviousHistoryPage(): void {
    this.historyPage.update((page) => Math.max(0, page - 1));
  }

  /**
   * Starts a background synchronization of every tracked player.
   */
  protected async synchronizeAll(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.synchronizeAllPlayers(), {
      state: this.syncAllState,
      successMessage: () => this.translation.translate('admin.operations.syncAll.accepted'),
    });
  }

  /**
   * Starts a background synchronization of the chosen player.
   */
  protected async synchronizePlayer(): Promise<void> {
    const playerId = this.selectedPlayerId();

    if (playerId === null) {
      const message = this.translation.translate('admin.operations.syncPlayer.noneSelected');

      this.syncPlayerState.set({ status: 'error', message });
      this.snackbar.error(message);

      return;
    }

    await this.commandRunner.run(() => this.adminApi.synchronizePlayer(playerId), {
      state: this.syncPlayerState,
      successMessage: () => this.translation.translate('admin.operations.syncPlayer.accepted'),
    });
  }

  /**
   * Opens the redraw dialog: the discarded challenges take their progress with them.
   */
  protected askForRedraw(): void {
    this.redrawDialogOpen.set(true);
  }

  /**
   * Closes the redraw dialog without drawing.
   */
  protected dismissRedraw(): void {
    this.redrawDialogOpen.set(false);
  }

  /**
   * Replaces the current week's challenge pack with a new draw.
   */
  protected async confirmRedraw(): Promise<void> {
    if (this.redrawing()) {
      return;
    }

    await this.commandRunner.run(() => this.adminApi.redrawCurrentChallenges(), {
      state: this.redrawState,
      busy: this.redrawing,
      successMessage: () => this.translation.translate('admin.operations.redraw.done'),
      onSuccess: () => this.redrawDialogOpen.set(false),
    });
  }

  /**
   * Runs the nightly tick now.
   */
  protected async runDailyTick(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.runDailyTick(), {
      state: this.dailyTickState,
      successMessage: () => this.translation.translate('admin.operations.dailyTick.done'),
    });
  }

  /**
   * Runs the weekly rollover now, closing every past week left open.
   */
  protected async runWeeklyRollover(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.runWeeklyRollover(), {
      state: this.rolloverState,
      successMessage: () => this.translation.translate('admin.operations.rollover.done'),
    });
  }
}
