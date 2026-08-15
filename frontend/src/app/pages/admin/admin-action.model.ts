/**
 * Lifecycle of one backoffice action, as the screen reports it.
 *
 * Kept per action rather than in a global notification area: the operator triggered this button,
 * and the answer belongs next to it. A page-level banner would make two actions run in a row
 * indistinguishable.
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
