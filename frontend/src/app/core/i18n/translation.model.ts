/**
 * Supported language.
 */
export type Language = 'fr' | 'en';

/**
 * Nested dictionary of translated strings.
 */
export interface TranslationDictionary {
  /**
   * Translated string, or a nested group of entries (plural branches included).
   */
  readonly [key: string]: string | TranslationDictionary;
}

/**
 * Lookup handed to pure helpers so they translate without injecting the service.
 */
export type TranslateFn = (
  key: string,
  params?: Readonly<Record<string, string | number>>,
) => string;
