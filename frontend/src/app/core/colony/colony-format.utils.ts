/**
 * BCP-47 locale backing the colony's number formatting, keyed by the app's own `Language` codes.
 *
 * Kept as a literal union rather than importing `Language`, so this module stays free of any
 * dependency on the i18n module — the same arrangement `challenge-format.utils` uses.
 */
const COLONY_LOCALES: Record<'fr' | 'en', string> = { fr: 'fr-FR', en: 'en-US' };

/**
 * Formats an inhabitant or materials count with its thousands separated (`2 690`, `12 470`).
 *
 * The colony's figures are four and five digits wide and are read as magnitudes at a glance, so
 * they are grouped exactly as the reward amounts elsewhere in the application are.
 *
 * @param value - The count to format.
 * @param language - The app language whose grouping separator to use.
 * @returns The grouped count.
 */
export function formatPopulation(value: number, language: 'fr' | 'en'): string {
  return new Intl.NumberFormat(COLONY_LOCALES[language]).format(Math.round(value));
}

/**
 * Formats a count with its sign always shown (`+92`, `-70`).
 *
 * What the night moved, and what a fight paid, are both read as directions before they are read as
 * amounts: a growing town and a shrinking one sit in exactly the same place on the page, so the sign
 * is what tells them apart rather than their position.
 *
 * @param value - The movement to format.
 * @param language - The app language whose grouping separator to use.
 * @returns The signed, grouped movement.
 */
export function formatSignedPopulation(value: number, language: 'fr' | 'en'): string {
  // `|| 0` collapses the negative zero `Math.round` returns for a movement just under nothing,
  // which `Intl` would otherwise print as `-0`.
  const rounded = Math.round(value) || 0;
  const sign = rounded > 0 ? '+' : '';

  return `${sign}${formatPopulation(rounded, language)}`;
}

/**
 * Formats a multiplier to two decimals (`1,71`).
 *
 * The turnout multiplier moves by sevenths, so one decimal would show five players out of seven and
 * six out of seven as the same figure.
 *
 * @param value - The value to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The value at two decimals.
 */
export function formatGauge(value: number, language: 'fr' | 'en'): string {
  return new Intl.NumberFormat(COLONY_LOCALES[language], {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Formats a multiplier as the factor it applies (`×1,71`).
 *
 * @param value - The multiplier to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The multiplier, prefixed with `×`.
 */
export function formatMultiplier(value: number, language: 'fr' | 'en'): string {
  return `×${formatGauge(value, language)}`;
}

/**
 * Formats a rate to one decimal (`8,3`).
 *
 * One decimal rather than the two a multiplier gets: the morale rate moves by twentieths of a point
 * over the run, and a second decimal on a figure captioned "per night" reads as a precision the
 * model does not have.
 *
 * @param value - The rate to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The rate at one decimal.
 */
export function formatRate(value: number, language: 'fr' | 'en'): string {
  return new Intl.NumberFormat(COLONY_LOCALES[language], {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value);
}
