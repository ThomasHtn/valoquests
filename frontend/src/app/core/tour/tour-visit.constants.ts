/**
 * `localStorage` key under which the completion of the guided tour is recorded.
 *
 * Kept separate from the landing page's own key: the landing is marked as soon as the compass is
 * clicked, whereas the tour is only marked once it has been walked through or explicitly skipped.
 */
export const STORAGE_KEY = 'valorant-tracker.tour-completed';
