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
