import { Language } from '../translation.model';
import { LOCALES } from './locale.constants';

/**
 * `Intl` locale of a language.
 */
export function resolveLocale(language: Language): string {
  return LOCALES[language];
}
