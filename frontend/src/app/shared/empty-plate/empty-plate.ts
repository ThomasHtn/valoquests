import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EmptyIllustration } from './empty-illustration/empty-illustration';
import { EmptyPlate as EmptyPlateContent } from './empty-plate.model';
import { READOUT_TONES } from './empty-plate.constants';

/**
 * Empty state as a mission plate: drawing, eyebrow, title, sentence and a readout strip.
 */
@Component({
  selector: 'app-empty-plate',
  imports: [EmptyIllustration],
  templateUrl: './empty-plate.html',
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
   * Dot and value classes of each readout tone, for the strip under the sentence.
   */
  protected readonly readoutTones = READOUT_TONES;
}
