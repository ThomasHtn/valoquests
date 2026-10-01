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

/**
 * Dictionary lookup handed to pure helpers, so they translate without injecting the service.
 */
export type TranslateFn = (
  key: string,
  params?: Readonly<Record<string, string | number>>,
) => string;
