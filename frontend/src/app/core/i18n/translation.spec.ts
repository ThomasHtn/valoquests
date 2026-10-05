import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Translation } from './translation';
import { LANGUAGE_STORAGE_KEY } from './translation.constants';

/**
 * Dictionary URL of a language, as the service resolves it.
 */
function dictionaryUrl(language: string): string {
  return new URL(`i18n/${language}.json`, document.baseURI).href;
}

describe('Translation', () => {
  let translation: Translation;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'fr');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    translation = TestBed.inject(Translation);
    httpMock = TestBed.inject(HttpTestingController);

    const initialized = translation.initialize();
    httpMock.expectOne(dictionaryUrl('fr')).flush({ greeting: 'Bonjour' });
    await initialized;
  });

  afterEach(() => {
    localStorage.removeItem(LANGUAGE_STORAGE_KEY);
    vi.restoreAllMocks();
  });

  it('switches language, storage and dictionary only once the new dictionary has loaded', async () => {
    const switched = translation.setLanguage('en');

    expect(translation.language()).toBe('fr');
    expect(translation.translate('greeting')).toBe('Bonjour');

    httpMock.expectOne(dictionaryUrl('en')).flush({ greeting: 'Hello' });
    await switched;

    expect(translation.language()).toBe('en');
    expect(translation.translate('greeting')).toBe('Hello');
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en');
  });

  it('keeps the old language when the new dictionary fails, and can retry', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    const failed = translation.setLanguage('en');
    httpMock.expectOne(dictionaryUrl('en')).flush(null, { status: 500, statusText: 'Error' });
    await failed;

    expect(translation.language()).toBe('fr');
    expect(translation.translate('greeting')).toBe('Bonjour');
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('fr');

    const retried = translation.setLanguage('en');
    httpMock.expectOne(dictionaryUrl('en')).flush({ greeting: 'Hello' });
    await retried;

    expect(translation.language()).toBe('en');
  });
});
