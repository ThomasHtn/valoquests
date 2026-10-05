/**
 * Lifecycle of a backoffice action, for button feedback; the outcome goes to the snackbar.
 */
type AdminActionStatus = 'idle' | 'running' | 'done' | 'error';

/**
 * State of one backoffice action.
 */
export interface AdminActionState {
  /**
   * Where the action stands.
   */
  readonly status: AdminActionStatus;

  /**
   * Translated outcome text, `''` while idle.
   */
  readonly message: string;
}
