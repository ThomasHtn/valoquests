/**
 * `sessionStorage` key under which the administrator key is held while the backoffice is open.
 *
 * Deliberately `sessionStorage` rather than `localStorage`, unlike every other preference this
 * application persists: the key grants every destructive operation the API exposes, so it must not
 * outlive the tab it was typed in. Closing the tab ends the session.
 */
export const ADMIN_KEY_STORAGE_KEY = 'valo-quests.admin-key';

/**
 * HTTP header the backend expects the administrator key in.
 */
export const ADMIN_KEY_HEADER = 'X-Admin-Key';

/**
 * Route the backoffice sends visitors to when they have no usable session.
 */
export const ADMIN_LOGIN_ROUTE = '/admin/login';

/**
 * Route the backoffice opens on once a session has been established.
 */
export const ADMIN_HOME_ROUTE = '/admin/operations';
