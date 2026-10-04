import { environment } from '@env/environment';

/**
 * `sessionStorage` key of the admin key; not `localStorage` so it dies with the tab.
 */
export const ADMIN_KEY_STORAGE_KEY = 'valo-quests.admin-key';

/**
 * HTTP header carrying the admin key.
 */
export const ADMIN_KEY_HEADER = 'X-Admin-Key';

/**
 * Route for visitors without a usable session.
 */
export const ADMIN_LOGIN_ROUTE = '/admin/login';

/**
 * Route opened once a session is established.
 */
export const ADMIN_HOME_ROUTE = '/admin/operations';

/**
 * URL prefix of the administration API.
 */
export const ADMIN_API_PREFIX = `${environment.apiBaseUrl}/admin`;
