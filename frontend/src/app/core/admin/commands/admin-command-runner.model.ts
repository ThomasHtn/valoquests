import { WritableSignal } from '@angular/core';
import { AdminActionState } from './admin-action.model';

/**
 * Options of one `AdminCommandRunner.run` call.
 */
export interface AdminCommandOptions<T> {
  /**
   * Receives the command's running/done/error state.
   */
  readonly state: WritableSignal<AdminActionState>;

  /**
   * Shared busy flag set while the command runs; optional when each action has its own state.
   */
  readonly busy?: WritableSignal<boolean>;

  /**
   * Builds the translated success message from the result.
   */
  readonly successMessage: (result: T) => string;

  /**
   * Side effect after success (closing a form, a dialog), never run on failure.
   */
  readonly onSuccess?: (result: T) => void;
}
