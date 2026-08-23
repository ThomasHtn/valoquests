import { Language } from './translation.model';

/**
 * Languages the application can be translated into.
 */
export const SUPPORTED_LANGUAGES: readonly Language[] = ['fr', 'en'];

/**
 * Language used when neither a stored choice nor the browser language is supported.
 */
export const DEFAULT_LANGUAGE: Language = 'fr';

/**
 * `localStorage` key under which the active language choice is persisted.
 */
export const STORAGE_KEY = 'valo-quests.language';
