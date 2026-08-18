import { inject, Service, WritableSignal } from '@angular/core';

import { Translation } from '@core/i18n/translation';
import { SnackbarService } from '@core/snackbar/snackbar';

import { AdminActionState } from './admin-action.model';
import { resolveAdminErrorMessage } from './admin-error.utils';

/**
 * Options controlling one {@link AdminCommandRunner.run} call.
 */
export interface AdminCommandOptions<T> {
  /**
   * Signal receiving the command's running/done/error state.
   */
  readonly state: WritableSignal<AdminActionState>;

  /**
   * Shared busy flag, set for the command's duration. Optional: a page whose actions each carry
   * their own {@link state} has no need for one.
   */
  readonly busy?: WritableSignal<boolean>;

  /**
   * Builds the already-translated success message from the command's result.
   */
  readonly successMessage: (result: T) => string;

  /**
   * Extra side effect run once the command has succeeded and {@link state} reflects it — closing a
   * form or a dialog, for instance. Never run when the command fails, so the operator is left
   * looking at whatever they were doing when it did.
   */
  readonly onSuccess?: (result: T) => void;
}

/**
 * Runs one backoffice command and reports its running/done/error outcome.
 *
 * Every admin page follows the same shape for a mutating command: flip busy on, run it, translate
 * the outcome into the page's action state, flip busy off. Centralizing it here is what stops each
 * page from re-deriving its own copy of that try/catch/finally. `state` still carries the outcome
 * message so a page can decide what to do while `running` (drive a spinner, lock a form) — but the
 * outcome itself, once the command settles, is reported through the global snackbar rather than
 * left for the page to render inline.
 */
@Service()
export class AdminCommandRunner {
  /**
   * i18n service used to resolve the fallback error message.
   */
  private readonly translation = inject(Translation);

  /**
   * Queues the outcome snackbar once the command settles.
   */
  private readonly snackbar = inject(SnackbarService);

  /**
   * Runs a command and reports its outcome through the given options.
   *
   * @param command - The command to run.
   * @param options - Where to report progress and what to do on success.
   */
  public async run<T>(command: () => Promise<T>, options: AdminCommandOptions<T>): Promise<void> {
    options.busy?.set(true);
    options.state.set({ status: 'running', message: '' });

    try {
      const result = await command();
      const message = options.successMessage(result);

      options.state.set({ status: 'done', message });
      this.snackbar.success(message);
      options.onSuccess?.(result);
    } catch (error: unknown) {
      const message = resolveAdminErrorMessage(
        error,
        this.translation.translate('admin.actionFailed'),
      );

      options.state.set({ status: 'error', message });
      this.snackbar.error(message);
    } finally {
      options.busy?.set(false);
    }
  }
}
