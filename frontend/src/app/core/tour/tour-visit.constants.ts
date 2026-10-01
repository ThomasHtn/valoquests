/**
 * `localStorage` key under which the completion of the guided tour is recorded.
 *
 * Kept separate from the landing page's own key: the landing is marked as soon as the compass is
 * clicked, whereas the tour is only marked once it has been walked through or explicitly skipped.
 */
export const STORAGE_KEY = 'valo-quests.tour-completed';

/**
 * Navigation state key that re-opens the guided tour after it has been completed.
 *
 * Carried in the router state rather than the URL, so a bookmark or a home-screen shortcut taken
 * during a replay cannot reopen the tour on every launch.
 */
export const REPLAY_STATE_KEY = 'replay';
