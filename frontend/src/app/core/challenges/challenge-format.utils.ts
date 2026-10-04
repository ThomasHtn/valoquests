import { resolveLocale } from '@core/i18n/format/locale.utils';
import { formatNumber } from '@core/i18n/format/number-format.utils';

/**
 * Damage with grouped thousands (`9 000`) so rewards read as magnitudes at a glance.
 */
export function formatDamage(damage: number, language: 'fr' | 'en'): string {
  return formatNumber(damage, resolveLocale(language));
}
