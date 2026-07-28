import { HttpClient } from '@angular/common/http';
import { effect, inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DEFAULT_LANGUAGE, STORAGE_KEY, SUPPORTED_LANGUAGES } from './translation.constants';
import { Language, TranslationDictionary } from './translation.model';

/**
 * Application-wide i18n service.
 *
 * Owns the active language, persists it to `localStorage`, and loads the
 * matching JSON dictionary used by {@link TranslatePipe} for lookups.
 */
@Service()
export class Translation {
  /**
   * Currently active language.
   */
  public readonly language = signal<Language>(this.detectInitialLanguage());

  /**
   * Languages the application can be translated into.
   */
  public readonly supportedLanguages = SUPPORTED_LANGUAGES;

  /**
   * HTTP client used to fetch language dictionaries.
   */
  private readonly http = inject(HttpClient);

  /**
   * Currently loaded translation dictionary, keyed by nested dot-separated paths.
   */
  private readonly dictionary = signal<TranslationDictionary>({});

  /**
   * Keeps the `<html lang>` attribute in sync with the active language,
   * so assistive technologies and browser features stay correct.
   */
  constructor() {
    effect(() => {
      document.documentElement.lang = this.language();
    });
  }

  /**
   * Loads the initial language dictionary.
   *
   * Awaited by an app initializer so the UI never flashes raw translation keys.
   *
   * @returns A promise that resolves once the dictionary has been loaded.
   */
  public initialize(): Promise<void> {
    return this.load(this.language());
  }

  /**
   * Switches the active language, persists the choice and loads its dictionary.
   *
   * @param language - The language to switch to.
   * @returns A promise that resolves once the new dictionary has been loaded.
   */
  public async setLanguage(language: Language): Promise<void> {
    if (language === this.language()) {
      return;
    }

    localStorage.setItem(STORAGE_KEY, language);
    this.language.set(language);
    await this.load(language);
  }

  /**
   * Resolves `key` against the currently loaded dictionary.
   *
   * @param key - Dot-separated dictionary path (e.g. `sidebar.nav.overview`).
   * @param params - Optional placeholder values substituted into the translated string.
   * @returns The translated string, or `key` itself when no translation is found.
   */
  public translate(key: string, params?: Readonly<Record<string, string | number>>): string {
    const value: unknown = key
      .split('.')
      .reduce<unknown>(
        (node, segment) =>
          typeof node === 'object' && node !== null
            ? (node as TranslationDictionary)[segment]
            : undefined,
        this.dictionary(),
      );

    if (typeof value !== 'string') {
      return key;
    }

    if (!params) {
      return value;
    }

    return Object.entries(params).reduce(
      (result, [name, replacement]) => result.replaceAll(`{{${name}}}`, String(replacement)),
      value,
    );
  }

  /**
   * Fetches the dictionary for `language` from `public/i18n` and stores it.
   *
   * Errors are caught rather than rethrown: {@link initialize} is awaited by an
   * app initializer, so a failed request would otherwise block bootstrap and
   * leave the application on a blank page instead of degrading to raw keys.
   *
   * @param language - The language whose dictionary should be loaded.
   */
  private async load(language: Language): Promise<void> {
    try {
      const dictionary = await firstValueFrom(
        this.http.get<TranslationDictionary>(`i18n/${language}.json`),
      );
      this.dictionary.set(dictionary);
    } catch (error) {
      console.error(`Failed to load the "${language}" translation dictionary.`, error);
    }
  }

  /**
   * Determines the language to use on startup.
   *
   * Prefers a previously stored choice, falls back to the browser language,
   * and defaults to {@link DEFAULT_LANGUAGE} when neither is supported.
   *
   * @returns The language to use on startup.
   */
  private detectInitialLanguage(): Language {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (this.isSupportedLanguage(stored)) {
      return stored;
    }

    const browserLanguage = navigator.language.split('-')[0];
    return this.isSupportedLanguage(browserLanguage) ? browserLanguage : DEFAULT_LANGUAGE;
  }

  /**
   * Type guard checking whether `value` is one of {@link SUPPORTED_LANGUAGES}.
   *
   * @param value - The candidate value to check.
   * @returns Whether `value` is a supported {@link Language}.
   */
  private isSupportedLanguage(value: string | null): value is Language {
    return value !== null && (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
  }
}
