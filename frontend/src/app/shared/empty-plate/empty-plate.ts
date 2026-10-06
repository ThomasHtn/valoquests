import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { EmptyIllustration } from './empty-illustration/empty-illustration';
import { READOUT_DOT_CLASSES } from './empty-plate.constants';
import { EmptyPlate as EmptyPlateContent } from './empty-plate.model';

/**
 * Empty state as a mission plate: drawing, eyebrow, title, sentence and a readout strip.
 */
@Component({
  selector: 'app-empty-plate',
  imports: [EmptyIllustration],
  templateUrl: './empty-plate.html',
  styleUrl: './empty-plate.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col items-center gap-2.5 text-center' },
})
export class EmptyPlate {
  /**
   * Translated plate content.
   */
  public readonly plate = input.required<EmptyPlateContent>();

  /**
   * Eyebrow tone: `creation` (brand) awaits someone, `waiting` (cyan) awaits data.
   */
  public readonly tone = input<'creation' | 'waiting'>('creation');

  /**
   * Dot modifier of each readout tone, for the strip under the sentence.
   */
  protected readonly readoutDotClasses = READOUT_DOT_CLASSES;
}
