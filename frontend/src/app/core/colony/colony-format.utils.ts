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
 * Formats a gauge value or a daily movement to one decimal (`72,0`, `11,4`).
 *
 * The gauges move by fractions of a point a day, so rounding them to whole numbers would show two
 * consecutive days as identical when they are not.
 *
 * @param value - The value to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The value at one decimal.
 */
export function formatGauge(value: number, language: 'fr' | 'en'): string {
  return new Intl.NumberFormat(COLONY_LOCALES[language], {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value);
}

/**
 * Formats a daily movement with its sign always shown (`+11,4`, `-9,0`).
 *
 * A gain and a loss sit side by side on the same line, so the sign is what tells them apart rather
 * than their position.
 *
 * @param value - The movement to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The signed movement at one decimal.
 */
export function formatSignedGauge(value: number, language: 'fr' | 'en'): string {
  const sign = value > 0 ? '+' : '';

  return `${sign}${formatGauge(value, language)}`;
}

/**
 * Formats a percentage to one decimal (`84,5 %`), or to none when it is whole.
 *
 * @param value - The percentage to format.
 * @param language - The app language whose decimal separator to use.
 * @returns The percentage, without its unit.
 */
export function formatPercentage(value: number, language: 'fr' | 'en'): string {
  return new Intl.NumberFormat(COLONY_LOCALES[language], {
    maximumFractionDigits: 1,
  }).format(value);
}
