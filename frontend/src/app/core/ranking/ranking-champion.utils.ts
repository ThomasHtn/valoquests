import { PageResponse } from '@core/http/page-response.model';
import { RankingHistoryWeek } from './ranking.model';

/**
 * Winner of the last finalized week (the reigning "Champion"), `null` while unknown.
 */
export function resolveChampionPlayerId(
  latestFinalizedWeek: PageResponse<RankingHistoryWeek> | null | undefined,
): number | null {
  return latestFinalizedWeek?.content[0]?.winnerPlayerId ?? null;
}
