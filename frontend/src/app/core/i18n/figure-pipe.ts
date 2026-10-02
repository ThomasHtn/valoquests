import { ChangeDetectorRef, effect, inject, Pipe, PipeTransform } from '@angular/core';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { Translation } from './translation';

/**
 * Groups a whole figure in the reader's notation (`1 234` in French, `1,234` in English), so a
 * template never prints a raw number beside formatted ones. Impure like `translate`, and for the
 * same reason: it must follow a language switch.
 */
@Pipe({
  name: 'figure',
  pure: false,
})
export class FigurePipe implements PipeTransform {
  private readonly translation = inject(Translation);

  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  constructor() {
    effect(() => {
      this.translation.language();
      this.changeDetectorRef.markForCheck();
    });
  }

  public transform(value: number): string {
    return formatDamage(value, this.translation.language());
  }
}
