import { ChangeDetectorRef, effect, inject, Pipe, PipeTransform } from '@angular/core';

import { Translation } from '../translation';
import { formatFigure } from './number-format.utils';

/**
 * Figure in the reader's notation; impure to follow a language switch.
 */
@Pipe({
  name: 'figure',
  pure: false,
})
export class FigurePipe implements PipeTransform {
  /**
   * Translation service, whose language picks the number notation.
   */
  private readonly translation = inject(Translation);

  /**
   * Change detector, marked on a language switch so the figure is reformatted.
   */
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  constructor() {
    effect(() => {
      this.translation.language();
      this.changeDetectorRef.markForCheck();
    });
  }

  /**
   * Formats `value` with digit grouping in the active language.
   */
  public transform(value: number): string {
    return formatFigure(value, this.translation.language());
  }
}
