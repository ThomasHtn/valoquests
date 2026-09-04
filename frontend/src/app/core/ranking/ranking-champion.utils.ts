import { PageResponse } from '@core/http/page-response.model';
import { RankingHistoryWeek } from './ranking.model';

/**
 * Resolves the id of the reigning weekly "Champion": the player who finished 1st in the most
 * recently finalized week.
 *
 * The title stays attached to that week forever once earned — stepping the leaderboard back to a
 * closed week decorates whoever won *that* one — but this resolver surfaces the *current* holder so
 * their name carries it everywhere else in the app, until the next week's winner supersedes them.
 *
 * @param latestFinalizedWeek - The single-week page fetched by `RankingApi.latestFinalizedWeek`,
 * or `null`/`undefined` while it has not loaded yet.
 * @returns The reigning champion's player id, or `null` while unknown or before any week has been
 * finalized.
 */
export function resolveChampionPlayerId(
  latestFinalizedWeek: PageResponse<RankingHistoryWeek> | null | undefined,
): number | null {
  return latestFinalizedWeek?.content[0]?.winnerPlayerId ?? null;
}
