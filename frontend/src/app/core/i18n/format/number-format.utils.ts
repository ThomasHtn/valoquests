import { resolveLocale } from './locale.utils';
import { Language } from '../translation.model';

/**
 * Number grouped by the locale but always with a decimal point, as Valorant prints stats.
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
 * Number with exactly `fractionDigits` decimals (`1.58` in every language).
 */
export function formatDecimal(value: number, language: Language, fractionDigits: number): string {
  return formatNumber(value, resolveLocale(language), {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

/**
 * Percentage on a 0 to 100 scale, in the reader's notation (`23.4 %` in French).
 */
export function formatPercent(percent: number, language: Language, fractionDigits = 0): string {
  return formatNumber(percent / 100, resolveLocale(language), {
    style: 'percent',
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

/**
 * Amount with grouped thousands (`9 000`), abbreviated (`27k`) when `compact` for a narrow slot.
 */
export function formatFigure(amount: number, language: Language, compact = false): string {
  const label = formatNumber(amount, resolveLocale(language), {
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: compact ? 1 : 2,
  });
  return compact ? label.replace(/[\s\u00a0\u202f]+/g, '') : label;
}
