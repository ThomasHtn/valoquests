/**
 * Upper bound of finalized weeks fetched in one call to {@link RankingApi.history} — the backend's
 * own maximum for `size` on `GET /api/rankings/history`.
 *
 * The tracked group is fixed and the calendar cadence is weekly (see the root CLAUDE.md), so almost
 * two years of history stay under this ceiling. Fetching it all in one request lets the leaderboard
 * step back through the weeks entirely client-side instead of round-tripping on every arrow press.
 */
export const RANKING_HISTORY_MAX_WEEKS = 100;
