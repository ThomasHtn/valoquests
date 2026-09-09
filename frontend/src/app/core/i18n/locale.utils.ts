import { Language } from './translation.model';

/**
 * BCP 47 locale used by `Intl` formatters for each supported language.
 */
const LOCALES: Readonly<Record<Language, string>> = { fr: 'fr-FR', en: 'en-US' };

/**
 * Resolves the `Intl` locale matching the active language.
 */
export function resolveLocale(language: Language): string {
  return LOCALES[language];
}
