import { AdminActionState } from './admin-action.model';

/**
 * State of an action not triggered yet.
 */
export const IDLE_ACTION: AdminActionState = { status: 'idle', message: '' };
