import { resolveLocale } from './locale.utils';
import { Language } from './translation.model';

/**
 * A number grouped as the locale groups it, but always written with a decimal point (`1 234.5` in
 * French): the squad reads game stats, where Valorant itself prints `1.58`.
 *
 * @param value - The number.
 * @param locale - The `Intl` locale, which decides the grouping.
 * @param options - The `Intl.NumberFormat` options.
 * @returns The formatted number.
 */
export function formatNumber(
  value: number,
  locale: string,
  options?: Intl.NumberFormatOptions,
): string {
  return new Intl.NumberFormat(locale, options)
    .formatToParts(value)
    .map((part) => (part.type === 'decimal' ? '.' : part.value))
    .join('');
}

/**
 * A number with a fixed count of decimals, in the reader's notation (`1.58` in every language).
 *
 * @param value - The number.
 * @param language - The active language.
 * @param fractionDigits - Decimals shown, always.
 * @returns The formatted number.
 */
export function formatDecimal(value: number, language: Language, fractionDigits: number): string {
  return formatNumber(value, resolveLocale(language), {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

/**
 * A percentage given on a 0 to 100 scale, in the reader's notation (`23.4 %` in French, `23.4%`
 * in English).
 *
 * @param percent - The percentage, 0 to 100.
 * @param language - The active language.
 * @param fractionDigits - Decimals shown, always.
 * @returns The formatted percentage.
 */
export function formatPercent(percent: number, language: Language, fractionDigits = 0): string {
  return formatNumber(percent / 100, resolveLocale(language), {
    style: 'percent',
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

/**
 * A figure in the reader's locale; abbreviated (`27k`) on request, for a ring's own fallback once
 * the exact figure runs wider than its disc.
 *
 * @param amount - The figure.
 * @param locale - The `Intl` locale.
 * @param compact - Whether to abbreviate it.
 * @returns The formatted figure.
 */
export function formatFigure(amount: number, locale: string, compact = false): string {
  const label = formatNumber(amount, locale, {
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: compact ? 1 : 2,
  });
  return compact ? label.replace(/[\s  ]+/g, '') : label;
}
