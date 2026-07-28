/**
 * Extracts the tag segment of a Riot ID (e.g. `"EUW"` from `"Kenshiro#EUW"`).
 *
 * @param riotId - The player's full Riot ID.
 * @returns The tag segment, or `null` when the Riot ID has no `#` separator.
 */
export function extractRiotTag(riotId: string): string | null {
  const separatorIndex = riotId.indexOf('#');
  return separatorIndex === -1 ? null : riotId.slice(separatorIndex + 1);
}

/**
 * Formats a win rate percentage for display, rounded to the nearest whole percent.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The formatted percentage, or an em dash when not yet synchronized.
 */
export function formatWinRate(winRate: number | null): string {
  return winRate === null ? '—' : `${Math.round(winRate)}%`;
}

/**
 * Formats a KDA ratio for display with two decimals.
 *
 * @param kda - The player's KDA ratio, or `null` when not yet synchronized.
 * @returns The formatted ratio, or an em dash when not yet synchronized.
 */
export function formatKda(kda: number | null): string {
  return kda === null ? '—' : kda.toFixed(2);
}

/**
 * Formats a headshot rate percentage for display with one decimal.
 *
 * @param headshotPercentage - The player's headshot rate percentage, or `null` when not yet
 * synchronized.
 * @returns The formatted percentage, or an em dash when not yet synchronized.
 */
export function formatHeadshotPercentage(headshotPercentage: number | null): string {
  return headshotPercentage === null ? '—' : `${headshotPercentage.toFixed(1)}%`;
}

/**
 * Formats an average combat/damage score (ACS or ADR) for display, rounded to the nearest whole
 * number.
 *
 * @param value - The score to format, or `null` when not yet synchronized.
 * @returns The formatted score, or an em dash when not yet synchronized.
 */
export function formatScore(value: number | null): string {
  return value === null ? '—' : `${Math.round(value)}`;
}
