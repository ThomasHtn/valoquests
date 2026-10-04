import { HttpClient } from '@angular/common/http';
import { effect, inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DEFAULT_LANGUAGE, STORAGE_KEY, SUPPORTED_LANGUAGES } from './translation.constants';
import { Language, TranslationDictionary } from './translation.model';
import { readStorage, writeStorage } from '@core/storage/safe-storage.utils';

/**
 * Active language, its persisted choice and its loaded dictionary.
 */
@Service()
export class Translation {
  /**
   * Active language.
   */
  public readonly language = signal<Language>(this.detectInitialLanguage());

  /**
   * Supported languages.
   */
  public readonly supportedLanguages = SUPPORTED_LANGUAGES;

  /**
   * HTTP client that fetches the dictionary files.
   */
  private readonly http = inject(HttpClient);

  /**
   * Loaded dictionary of the active language, empty until the first load.
   */
  private readonly dictionary = signal<TranslationDictionary>({});

  constructor() {
    // `<html lang>` follows the language, for assistive technologies.
    effect(() => {
      document.documentElement.lang = this.language();
    });
  }

  /**
   * Loads the initial dictionary; awaited by an app initializer so no raw key flashes.
   */
  public initialize(): Promise<void> {
    return this.load(this.language());
  }

  /**
   * Switches the language, persists it and loads its dictionary.
   */
  public async setLanguage(language: Language): Promise<void> {
    if (language === this.language()) {
      return;
    }

    writeStorage(STORAGE_KEY, language);
    this.language.set(language);
    await this.load(language);
  }

  /**
   * Translated string, or `key` when missing; a numeric `count` picks the `one`/`other` branch.
   */
  public translate(key: string, params?: Readonly<Record<string, string | number>>): string {
    const entry: unknown = key
      .split('.')
      .reduce<unknown>(
        (node, segment) =>
          typeof node === 'object' && node !== null
            ? (node as TranslationDictionary)[segment]
            : undefined,
        this.dictionary(),
      );

    const value = this.resolvePluralBranch(entry, params?.['count']);
    if (value === null) {
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
   * Every string under `key`, lower-cased into one searchable blob (`''` for an unknown key).
   */
  public searchText(key: string): string {
    const entry: unknown = key
      .split('.')
      .reduce<unknown>(
        (node, segment) =>
          typeof node === 'object' && node !== null
            ? (node as TranslationDictionary)[segment]
            : undefined,
        this.dictionary(),
      );

    const collect = (node: unknown): readonly string[] => {
      if (typeof node === 'string') {
        return [node];
      }

      return typeof node === 'object' && node !== null
        ? Object.values(node as TranslationDictionary).flatMap(collect)
        : [];
    };

    return collect(entry).join(' ').toLowerCase();
  }

  /**
   * String to render, `null` if none; French treats 0 as singular, English does not.
   */
  private resolvePluralBranch(entry: unknown, count: string | number | undefined): string | null {
    if (typeof entry === 'string') {
      return entry;
    }

    if (typeof entry !== 'object' || entry === null || typeof count !== 'number') {
      return null;
    }

    const branches = entry as TranslationDictionary;
    const isSingular = this.language() === 'fr' ? Math.abs(count) < 2 : Math.abs(count) === 1;
    const branch = branches[isSingular ? 'one' : 'other'];

    return typeof branch === 'string' ? branch : null;
  }

  /**
   * Fetches a dictionary against `document.baseURI`, so nested URLs and sub-path deployments work.
   * Errors are swallowed: a rethrow would block bootstrap instead of degrading to raw keys.
   */
  private async load(language: Language): Promise<void> {
    try {
      const dictionary = await firstValueFrom(
        this.http.get<TranslationDictionary>(
          new URL(`i18n/${language}.json`, document.baseURI).href,
        ),
      );
      // A quicker toggle back may have superseded this request.
      if (language === this.language()) {
        this.dictionary.set(dictionary);
      }
    } catch (error) {
      console.error(`Failed to load the "${language}" translation dictionary.`, error);
    }
  }

  /**
   * Startup language: stored choice, else browser language, else {@link DEFAULT_LANGUAGE}.
   */
  private detectInitialLanguage(): Language {
    const stored = readStorage(STORAGE_KEY);
    if (this.isSupportedLanguage(stored)) {
      return stored;
    }

    const browserLanguage = navigator.language.split('-')[0];
    return this.isSupportedLanguage(browserLanguage) ? browserLanguage : DEFAULT_LANGUAGE;
  }

  /**
   * Whether `value` is a supported language.
   */
  private isSupportedLanguage(value: string | null): value is Language {
    return value !== null && (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
  }
}
