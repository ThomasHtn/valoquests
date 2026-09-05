/**
 * BCP-47 locale backing {@link formatDamage}'s thousands grouping, keyed by the app's own
 * `Language` codes (kept as a literal union here rather than importing `Language` from
 * `core/i18n`, so this module stays free of any dependency on the i18n module).
 */
const DAMAGE_GROUPING_LOCALES: Record<'fr' | 'en', string> = { fr: 'fr-FR', en: 'en-US' };

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
  return new Intl.NumberFormat(DAMAGE_GROUPING_LOCALES[language]).format(damage);
}

/**
 * Formats a squad bonus as the multiplier it applies to a challenge's base damage (`×1,2`).
 *
 * A multiplier rather than the bonus amount: the base damage stays the figure the card advertises,
 * and what the squad adds reads as something done to it. The percentage itself is resolved by the
 * backend from the week's ruleset — this only turns it into a number a reader recognises.
 *
 * @param bonusPercent - The squad bonus, as a percentage of the base damage.
 * @param language - The app language whose decimal separator to use.
 * @returns The multiplier, or `null` when no bonus is earned yet.
 */
export function formatSquadMultiplier(bonusPercent: number, language: 'fr' | 'en'): string | null {
  if (bonusPercent <= 0) {
    return null;
  }

  const formatted = new Intl.NumberFormat(DAMAGE_GROUPING_LOCALES[language], {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(1 + bonusPercent / 100);

  return `×${formatted}`;
}
