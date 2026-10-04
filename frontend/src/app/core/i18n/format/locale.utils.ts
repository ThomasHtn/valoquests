import { LOCALES } from './locale.constants';
import { Language } from '../translation.model';

/**
 * `Intl` locale of a language.
 */
export function resolveLocale(language: Language): string {
  return LOCALES[language];
}
