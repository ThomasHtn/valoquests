import { PlayerSummary } from './player-summary.model';

/**
 * Resolves the most recent successful synchronization among `players`.
 *
 * @param players - Tracked players' summaries.
 * @returns The latest `lastSuccessfulSynchronizationAt` instant, as an ISO-8601 string, or `null`
 * when no player has been synchronized successfully yet.
 */
export function resolveLatestSynchronization(players: readonly PlayerSummary[]): string | null {
  return players.reduce<string | null>((latest, player) => {
    const candidate = player.lastSuccessfulSynchronizationAt;
    if (!candidate) {
      return latest;
    }

    return !latest || new Date(candidate).getTime() > new Date(latest).getTime()
      ? candidate
      : latest;
  }, null);
}
