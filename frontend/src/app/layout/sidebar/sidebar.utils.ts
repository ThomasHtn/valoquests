import { PlayerSummary } from '@core/players/player-summary.model';

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

/**
 * Formats an ISO-8601 instant as `DD/MM/YYYY - HH:mm` in the browser's local time.
 *
 * @param instant - The instant to format, as an ISO-8601 string.
 * @returns The formatted timestamp.
 */
export function formatSynchronizationTimestamp(instant: string): string {
  const date = new Date(instant);
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${day}/${month}/${date.getFullYear()} - ${hours}:${minutes}`;
}
