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
 * Renders an em dash when the score is unavailable, as {@link formatWinRate} and {@link formatKda}
 * already do: some game modes do not report one, and an empty cell cannot be told apart from data
 * that has not loaded. The guard is on finiteness rather than on `null`, since the field is absent
 * from the payload for those modes and `Math.round(undefined)` would otherwise print `NaN`.
 *
 * @param value - The score to format, when reported.
 * @returns The formatted score, or an em dash when unavailable.
 */
export function formatScore(value: number | null): string {
  return Number.isFinite(value) ? `${Math.round(value as number)}` : '—';
}
