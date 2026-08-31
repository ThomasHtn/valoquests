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
   * An entry may be a plain string, or — when the sentence changes with a quantity — an object of
   * `one` and `other` branches picked from a numeric `count` parameter (see
   * {@link resolvePluralBranch}). That is what keeps the interface off `"{{count}} joueur(s)"`,
   * which reads as an unfinished string rather than as a sentence.
   *
   * @param key - Dot-separated dictionary path (e.g. `sidebar.nav.overview`).
   * @param params - Optional placeholder values substituted into the translated string. A numeric
   * `count` additionally selects the plural branch of a pluralized entry.
   * @returns The translated string, or `key` itself when no translation is found.
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
   * Every translated string under `key`, flattened and lower-cased into one searchable blob.
   *
   * The rules page searches its own content, which is entirely dictionary text: rather than
   * duplicating that prose into a keyword list that would drift from it on the first edit, the
   * search reads the same entries the page renders. Placeholders are left as they are — nobody
   * searches for `{{count}}`, and stripping them would cost a pass for nothing.
   *
   * Returns `''` for an unknown key, so a caller can index a section that has no prose without
   * branching.
   *
   * @param key - Dot-separated path of the subtree to flatten (e.g. `rules.sections.boss`).
   * @returns The subtree's strings joined by a space, lower-cased.
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
   * Narrows a dictionary entry to the single string to render.
   *
   * A pluralized entry carries `one` and `other` branches; which one applies depends on the active
   * language, not only on the count. French treats 0 as singular ("0 joueur"), English does not
   * ("0 players") — hence the rule below rather than a shared `count === 1`.
   *
   * @param entry - The raw dictionary entry, of unknown shape.
   * @param count - The `count` parameter passed to {@link translate}, if any.
   * @returns The string to render, or `null` when the entry is not renderable.
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
   * Fetches the dictionary for `language` from `public/i18n` and stores it.
   *
   * Resolved against `document.baseURI` rather than requested relatively: a relative path is
   * resolved against the *current route*, so entering the application on a nested URL
   * (`/players/12`, `/admin/operations`) asked for `/players/i18n/fr.json` and every screen
   * rendered raw keys. An absolute `/i18n/…` would fix that but break a deployment under a
   * sub-path, which the `<base href>` is what tracks.
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
        this.http.get<TranslationDictionary>(
          new URL(`i18n/${language}.json`, document.baseURI).href,
        ),
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
