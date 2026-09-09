import { WritableSignal } from '@angular/core';
import { AdminActionState } from './admin-action.model';

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
