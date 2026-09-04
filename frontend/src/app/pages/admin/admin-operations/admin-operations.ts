import { Component, computed, effect, inject, signal } from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { AdminApi } from '@core/admin/admin-api';
import { AdminCommandRunner } from '@core/admin/admin-command-runner';
import {
  IN_FLIGHT_SYNCHRONIZATION_STATUSES,
  SynchronizationExecution,
} from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { SnackbarService } from '@core/snackbar/snackbar';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { formatSynchronizationTimestamp } from '@layout/sidebar/sidebar.utils';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { PageHeader } from '@layout/page-header/page-header';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { SectionLabel } from '@shared/section-label/section-label';
import { StatusBadge, StatusBadgeTone } from '@shared/status-badge/status-badge';
import { AdminActionCard } from '../admin-action-card/admin-action-card';

/**
 * Delay between two polls of the running synchronization, in milliseconds.
 *
 * A synchronization walks the Henrik match history under a rate limit of a few dozen requests per
 * minute, so its counters move in steps of seconds at best. Polling faster would only multiply
 * requests against a status that has not changed.
 */
const SYNCHRONIZATION_POLL_INTERVAL_MS = 3_000;

/**
 * Backoffice operations screen.
 *
 * Gathers every command that repairs or refreshes the tracker: synchronizing the squad or one
 * player, rebuilding progress and ranking, drawing a week the Monday rollover failed to open,
 * running that rollover in full when it never fired at all, replaying the campaign, and throwing the
 * week's challenge pack away for a new one.
 *
 * A synchronization runs in the background and outlives the request that started it, so the page
 * opens on the state of the latest run and polls it while it is in flight. That poll is the only
 * feedback the operator gets: the command itself answers `202` and says nothing about how the walk
 * went.
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
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminOperations {
  /**
   * Data-access service backing every command and the synchronization resource.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * i18n service used to resolve outcome messages, which are built here rather than in templates.
   */
  private readonly translation = inject(Translation);

  /**
   * Runs each command below and reports its running/done/error outcome in the card that triggered
   * it.
   */
  private readonly commandRunner = inject(AdminCommandRunner);

  /**
   * Queues the "no player selected" snackbar, the one outcome on this page the shared runner never
   * sees since it is rejected before any command runs.
   */
  private readonly snackbar = inject(SnackbarService);

  /**
   * Resource holding every tracked player, backing the per-player synchronization picker.
   */
  protected readonly playersResource = this.adminApi.players;

  /**
   * Resource holding the most recent synchronization execution.
   */
  protected readonly synchronizationResource = this.adminApi.latestSynchronization;

  /**
   * Latest synchronization execution, or `null` when none has ever run.
   */
  protected readonly synchronization = computed(() =>
    resourceValue(this.synchronizationResource, undefined),
  );

  /**
   * Whether a synchronization is currently in flight.
   *
   * Also what disables both synchronization buttons: the backend refuses a concurrent run with a
   * 409, and letting the operator trigger one only to be told no is a worse answer than the button
   * being visibly unavailable.
   */
  protected readonly synchronizing = computed(() => {
    const execution = this.synchronization();

    return execution !== undefined && IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(execution.status);
  });

  /**
   * Player the per-player synchronization targets, or `null` while none is chosen.
   */
  protected readonly selectedPlayerId = signal<number | null>(null);

  /**
   * Options offered by the per-player synchronization picker.
   *
   * Archived players are left out: they are off the roster and the backend does not synchronize
   * them, so offering one would promise a run that imports nothing.
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
   * State of the squad-wide synchronization command.
   */
  protected readonly syncAllState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the per-player synchronization command.
   */
  protected readonly syncPlayerState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the progress recalculation command.
   */
  protected readonly recalculateState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the challenge redraw command.
   */
  protected readonly redrawState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Whether the redraw confirmation dialog is on screen.
   */
  protected readonly redrawDialogOpen = signal(false);

  /**
   * Whether the redraw is currently running, which locks the dialog's buttons.
   */
  protected readonly redrawing = signal(false);

  /**
   * State of the weekly selection command.
   */
  protected readonly weekSelectionState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the weekly rollover command.
   */
  protected readonly rolloverState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * State of the campaign replay command.
   */
  protected readonly campaignReplayState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Translated label of the latest execution's status.
   */
  protected readonly statusLabel = computed(() => {
    const execution = this.synchronization();

    return execution === undefined
      ? this.translation.translate('admin.operations.status.none')
      : this.translation.translate(`admin.operations.status.${execution.status}`);
  });

  /**
   * Timestamp the latest execution started at, or `''` when unknown.
   *
   * Tested for emptiness rather than for `null`: the backend leaves null properties out of its
   * payloads, so an execution that never started arrives with the field missing rather than null.
   */
  protected readonly startedLabel = computed(() => {
    const startedAt = this.synchronization()?.startedAt;

    return startedAt ? formatSynchronizationTimestamp(startedAt) : '';
  });

  /**
   * Zero-based page of the synchronization history on screen.
   */
  protected readonly historyPage = signal(0);

  /**
   * Reactive resource fetching the requested page of synchronization history.
   */
  protected readonly historyResource = this.adminApi.synchronizationHistory(this.historyPage);

  /**
   * The page's executions, most recent first, or `[]` while loading.
   */
  protected readonly historyRows = computed(
    () => resourceValue(this.historyResource, undefined)?.content ?? [],
  );

  /**
   * Whether a page past the one on screen exists.
   */
  protected readonly hasNextHistoryPage = computed(() => {
    const page = resourceValue(this.historyResource, undefined);

    return page !== undefined && this.historyPage() + 1 < page.totalPages;
  });

  /**
   * Identifier of the execution whose per-player results are open, or `null` while none is.
   */
  protected readonly expandedExecutionId = signal<number | null>(null);

  /**
   * Reactive resource fetching {@link expandedExecutionId}'s per-player results, fetched only
   * once a reader actually opens a row.
   */
  protected readonly executionDetailsResource = this.adminApi.synchronizationDetails(
    this.expandedExecutionId,
  );

  /**
   * {@link expandedExecutionId}'s per-player results, or `[]` while collapsed or loading.
   */
  protected readonly executionDetailsPlayers = computed(
    () => resourceValue(this.executionDetailsResource, undefined)?.players ?? [],
  );

  /**
   * Formats a synchronization's start timestamp, exposed to the template.
   */
  protected readonly formatTimestamp = formatSynchronizationTimestamp;

  /**
   * Polls the running synchronization until it settles.
   *
   * The interval is created and torn down by the same effect, so it only exists while there is
   * something to watch: a finished run stops the poll rather than leaving it turning against a
   * status that can no longer change.
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
   * Resolves a history row's status badge tone.
   *
   * @param status - The execution's own status.
   * @returns The tone to render the badge in.
   */
  protected historyStatusTone(status: SynchronizationExecution['status']): StatusBadgeTone {
    if (IN_FLIGHT_SYNCHRONIZATION_STATUSES.includes(status)) {
      return 'brand';
    }

    return status === 'FAILED' || status === 'PARTIAL' ? 'danger' : 'neutral';
  }

  /**
   * Opens one execution's per-player results, or closes it if it is already open.
   *
   * @param executionId - The execution's own identifier.
   */
  protected toggleExecution(executionId: number): void {
    this.expandedExecutionId.update((current) => (current === executionId ? null : executionId));
  }

  /**
   * Steps the history to the next, older page.
   */
  protected loadNextHistoryPage(): void {
    if (this.hasNextHistoryPage()) {
      this.historyPage.update((page) => page + 1);
    }
  }

  /**
   * Steps the history back to the previous, more recent page.
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
   * Rebuilds the current week's challenge progress and the weekly ranking.
   */
  protected async recalculateProgress(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.recalculateProgress(), {
      state: this.recalculateState,
      successMessage: () => this.translation.translate('admin.operations.recalculate.done'),
    });
  }

  /**
   * Opens the redraw confirmation dialog.
   *
   * The one command on this page that destroys data rather than rebuilding it, so it is the one
   * asked for twice: the progress recorded against the discarded challenges goes with them.
   */
  protected askForRedraw(): void {
    this.redrawDialogOpen.set(true);
  }

  /**
   * Closes the redraw confirmation dialog without drawing anything.
   */
  protected dismissRedraw(): void {
    this.redrawDialogOpen.set(false);
  }

  /**
   * Discards the current week's challenge pack and draws a new one in its place.
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
   * Draws the current week's missing challenges and its boss encounter.
   */
  protected async selectCurrentWeek(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.selectCurrentWeek(), {
      state: this.weekSelectionState,
      successMessage: () => this.translation.translate('admin.operations.weekSelection.done'),
    });
  }

  /**
   * Runs the weekly rollover now, closing every past week the Monday job left open.
   */
  protected async runWeeklyRollover(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.runWeeklyRollover(), {
      state: this.rolloverState,
      successMessage: () => this.translation.translate('admin.operations.rollover.done'),
    });
  }

  /**
   * Replays the running campaign from its first day.
   */
  protected async replayCampaign(): Promise<void> {
    await this.commandRunner.run(() => this.adminApi.replayCampaign(), {
      state: this.campaignReplayState,
      successMessage: () => this.translation.translate('admin.operations.campaignReplay.done'),
    });
  }
}
