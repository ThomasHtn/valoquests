import { ChangeDetectorRef, effect, inject, Pipe, PipeTransform } from '@angular/core';

import { Translation } from './translation';

/**
 * Translates a dictionary key; impure because the result depends on the language signal.
 */
@Pipe({
  name: 'translate',
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  /**
   * Translation service that resolves the keys.
   */
  private readonly translation = inject(Translation);

  /**
   * Change detector, marked on a language switch so the text is retranslated.
   */
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  constructor() {
    effect(() => {
      this.translation.language();
      this.changeDetectorRef.markForCheck();
    });
  }

  /**
   * Translated string, or `key` itself when missing.
   */
  public transform(key: string, params?: Readonly<Record<string, string | number>>): string {
    return this.translation.translate(key, params);
  }
}
