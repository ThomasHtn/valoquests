import { resolveLocale } from '@core/i18n/locale.utils';
import { formatNumber } from '@core/i18n/number-format.utils';

/**
 * Formats a damage amount with its thousands separated, as the design shows it (`9 000` rather
 * than `9000`): the four- and five-digit rewards are read as magnitudes at a glance, not parsed
 * digit by digit.
 *
 * @param damage - The damage amount to format.
 * @param language - The app language whose grouping separator to use.
 * @returns The grouped amount.
 */
export function formatDamage(damage: number, language: 'fr' | 'en'): string {
  return formatNumber(damage, resolveLocale(language));
}

/**
 * Formats a squad bonus as the multiplier it applies to a challenge's base damage (`×1.2`).
 *
 * A multiplier rather than the bonus amount: the base damage stays the figure the card advertises,
 * and what the squad adds reads as something done to it. The percentage itself is resolved by the
 * backend from the week's ruleset — this only turns it into a number a reader recognises.
 *
 * @param bonusPercent - The squad bonus, as a percentage of the base damage.
 * @param language - The app language whose grouping to use.
 * @returns The multiplier, or `null` when no bonus is earned yet.
 */
export function formatSquadMultiplier(bonusPercent: number, language: 'fr' | 'en'): string | null {
  if (bonusPercent <= 0) {
    return null;
  }

  const formatted = formatNumber(1 + bonusPercent / 100, resolveLocale(language), {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  });

  return `×${formatted}`;
}
