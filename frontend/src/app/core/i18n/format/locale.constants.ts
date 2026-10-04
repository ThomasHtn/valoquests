import { Language } from '../translation.model';

/**
 * BCP 47 locale of each language, for `Intl` formatters.
 */
export const LOCALES: Readonly<Record<Language, string>> = { fr: 'fr-FR', en: 'en-US' };
