import { ChangeDetectorRef, effect, inject, Pipe, PipeTransform } from '@angular/core';

import { Translation } from './translation';

/**
 * Impure pipe translating a dictionary key into the active language's string.
 *
 * Declared `pure: false` because the lookup depends on the current language
 * signal rather than on the `key`/`params` arguments Angular tracks by default.
 */
@Pipe({
  name: 'translate',
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  /**
   * i18n service providing the active language and dictionary lookups.
   */
  private readonly translation = inject(Translation);
  /**
   * Used to mark the host view for check when the active language changes.
   */
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  /**
   * Subscribes to language changes so the host view re-renders even though
   * the dictionary lookup itself is not part of the pipe's tracked arguments.
   */
  constructor() {
    effect(() => {
      this.translation.language();
      this.changeDetectorRef.markForCheck();
    });
  }

  /**
   * Resolves `key` against the active translation dictionary.
   *
   * @param key - Dot-separated dictionary path (e.g. `sidebar.nav.overview`).
   * @param params - Optional placeholder values substituted into the translated string.
   * @returns The translated string, or `key` itself when no translation is found.
   */
  public transform(key: string, params?: Readonly<Record<string, string | number>>): string {
    return this.translation.translate(key, params);
  }
}
