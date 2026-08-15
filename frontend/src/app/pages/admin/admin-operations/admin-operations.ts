import { Component, computed, effect, inject, signal } from '@angular/core';

import { AdminApi } from '@core/admin/admin-api';
import { resolveAdminErrorMessage } from '@core/admin/admin-error.utils';
import { IN_FLIGHT_SYNCHRONIZATION_STATUSES } from '@core/admin/admin.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { formatSynchronizationTimestamp } from '@layout/sidebar/sidebar.utils';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { AdminActionCard } from '../admin-action-card/admin-action-card';
import { AdminActionState, IDLE_ACTION } from '../admin-action.model';

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
 * player, rebuilding progress and ranking, and drawing a week the Monday rollover failed to open.
 *
 * A synchronization runs in the background and outlives the request that started it, so the page
 * opens on the state of the latest run and polls it while it is in flight. That poll is the only
 * feedback the operator gets: the command itself answers `202` and says nothing about how the walk
 * went.
 */
@Component({
  selector: 'app-admin-operations',
  imports: [TranslatePipe, AdminActionCard, SectionDivider, Select],
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
   * State of the weekly selection command.
   */
  protected readonly weekSelectionState = signal<AdminActionState>(IDLE_ACTION);

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
   * Starts a background synchronization of every tracked player.
   */
  protected async synchronizeAll(): Promise<void> {
    await this.run(this.syncAllState, 'admin.operations.syncAll.accepted', () =>
      this.adminApi.synchronizeAllPlayers(),
    );
  }

  /**
   * Starts a background synchronization of the chosen player.
   */
  protected async synchronizePlayer(): Promise<void> {
    const playerId = this.selectedPlayerId();

    if (playerId === null) {
      this.syncPlayerState.set({
        status: 'error',
        message: this.translation.translate('admin.operations.syncPlayer.noneSelected'),
      });

      return;
    }

    await this.run(this.syncPlayerState, 'admin.operations.syncPlayer.accepted', () =>
      this.adminApi.synchronizePlayer(playerId),
    );
  }

  /**
   * Rebuilds the current week's challenge progress and the weekly ranking.
   */
  protected async recalculateProgress(): Promise<void> {
    await this.run(this.recalculateState, 'admin.operations.recalculate.done', () =>
      this.adminApi.recalculateProgress(),
    );
  }

  /**
   * Draws the current week's missing challenges and its boss encounter.
   */
  protected async selectCurrentWeek(): Promise<void> {
    await this.run(this.weekSelectionState, 'admin.operations.weekSelection.done', () =>
      this.adminApi.selectCurrentWeek(),
    );
  }

  /**
   * Runs one command and reports its outcome in the card that triggered it.
   *
   * @param state - Signal holding the card's state.
   * @param successKey - Translation key of the success message.
   * @param command - The command to run.
   */
  private async run(
    state: { set: (value: AdminActionState) => void },
    successKey: string,
    command: () => Promise<void>,
  ): Promise<void> {
    state.set({ status: 'running', message: '' });

    try {
      await command();
      state.set({ status: 'done', message: this.translation.translate(successKey) });
    } catch (error: unknown) {
      state.set({
        status: 'error',
        message: resolveAdminErrorMessage(error, this.translation.translate('admin.actionFailed')),
      });
    }
  }
}
