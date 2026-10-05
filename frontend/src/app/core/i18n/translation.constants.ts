import { Language } from './translation.model';

/**
 * Supported languages.
 */
export const SUPPORTED_LANGUAGES: readonly Language[] = ['fr', 'en'];

/**
 * Fallback when neither the stored choice nor the browser language is supported.
 */
export const DEFAULT_LANGUAGE: Language = 'fr';

/**
 * `localStorage` key of the language choice.
 */
export const LANGUAGE_STORAGE_KEY = 'valo-quests.language';
