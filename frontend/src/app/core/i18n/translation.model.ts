/**
 * Language a translation dictionary can be loaded for.
 */
export type Language = 'fr' | 'en';

/**
 * Recursive dictionary of translated strings, keyed by nested dot-separated paths.
 */
export interface TranslationDictionary {
  readonly [key: string]: string | TranslationDictionary;
}
