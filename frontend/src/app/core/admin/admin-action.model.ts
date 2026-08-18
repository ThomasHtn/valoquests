/**
 * Lifecycle of one backoffice action, as the screen reports it.
 *
 * `status` still drives per-button feedback (a spinner while `running`, staying disabled for the
 * duration); the outcome once the action settles is reported through the global snackbar instead
 * of `message`, which callers may still read but no longer need to render.
 */
export type AdminActionStatus = 'idle' | 'running' | 'done' | 'error';

/**
 * State of one backoffice action.
 */
export interface AdminActionState {
  readonly status: AdminActionStatus;

  /**
   * Already-translated outcome text, or `''` while idle.
   */
  readonly message: string;
}

/**
 * State of an action nothing has triggered yet.
 */
export const IDLE_ACTION: AdminActionState = { status: 'idle', message: '' };
