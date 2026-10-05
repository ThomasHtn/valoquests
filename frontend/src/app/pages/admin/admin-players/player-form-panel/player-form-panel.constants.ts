import { AdminPlayerStatus } from '@core/admin/players/admin-player.model';

/**
 * Portrait value for "no avatar", a real option since an unset `app-select` reads as unchosen.
 */
export const NO_PORTRAIT = '';

/**
 * Statuses a new player can start with; archiving only follows a removal.
 */
export const INITIAL_STATUSES: readonly AdminPlayerStatus[] = ['ACTIVE', 'INACTIVE'];
