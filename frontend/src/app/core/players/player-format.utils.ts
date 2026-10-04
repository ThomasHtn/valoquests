import { formatDecimal, formatPercent } from '@core/i18n/format/number-format.utils';
import { Language } from '@core/i18n/translation.model';

/**
 * Tag of a Riot ID (`"EUW"` from `"Kenshiro#EUW"`), `null` without `#`.
 */
export function extractRiotTag(riotId: string): string | null {
  const separatorIndex = riotId.indexOf('#');
  return separatorIndex === -1 ? null : riotId.slice(separatorIndex + 1);
}

/**
 * Win rate rounded to the percent, a dash when `null`.
 */
export function formatWinRate(winRate: number | null, language: Language): string {
  return winRate === null ? '—' : formatPercent(winRate, language);
}

/**
 * KDA with two decimals, a dash when `null` or not finite.
 */
export function formatKda(kda: number | null, language: Language): string {
  return kda === null || !Number.isFinite(kda) ? '—' : formatDecimal(kda, language, 2);
}

/**
 * Headshot rate with one decimal, a dash when `null`.
 */
export function formatHeadshotPercentage(
  headshotPercentage: number | null,
  language: Language,
): string {
  return headshotPercentage === null ? '—' : formatPercent(headshotPercentage, language, 1);
}

/**
 * Rounded ACS or ADR, a dash when missing; checks finiteness as some modes omit the field.
 */
export function formatScore(value: number | null): string {
  return Number.isFinite(value) ? `${Math.round(value as number)}` : '—';
}
