/**
 * `localStorage` key under which the last week whose recap was dismissed is recorded.
 *
 * Holds the dismissed week's Monday rather than a boolean, unlike the landing and tour flags beside
 * it: the recap is not a one-time briefing but a weekly one, so "already seen" has to mean "seen
 * *this* week". A stored date also makes the record self-expiring — Monday's rollover names a new
 * week, the stored one stops matching, and the panel comes back without anything having to clear it.
 */
export const STORAGE_KEY = 'valo-quests.week-recap-dismissed';
