import { inject, Service } from '@angular/core';
import { Translation } from '@core/i18n/translation';
import { SnackbarQueue } from '@core/snackbar/snackbar';
import { resolveAdminErrorMessage } from '../admin-error.utils';
import { AdminCommandOptions } from './admin-command-runner.model';

/**
 * Runs a backoffice command, tracks its state and reports the outcome in the snackbar.
 */
@Service()
export class AdminCommandRunner {
  /**
   * Resolves the fallback error message.
   */
  private readonly translation = inject(Translation);

  /**
   * Shows the outcome once the command settles.
   */
  private readonly snackbar = inject(SnackbarQueue);

  /**
   * Runs a command and reports its outcome through `options`.
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
