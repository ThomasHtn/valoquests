import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EmptyIllustration } from './empty-illustration';
import { EmptyPlate as EmptyPlateContent } from './empty-plate.model';
import { READOUT_TONES } from './empty-plate.constants';

/**
 * An empty state shaped like a mission plate: a line drawing, an eyebrow naming the situation, a
 * title, the sentence, then a strip of readouts.
 *
 * The four public pages used to greet "no campaign yet" with a hexagon and one grey sentence,
 * the same shrug as a filter matching nothing. Centred and illustrated, the plate says what the
 * squad is waiting on and what already runs — the ranking, the Monday draw — so an empty page still
 * reads as the mission's state rather than as an absence.
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
   * What the plate shows, already translated.
   */
  public readonly plate = input.required<EmptyPlateContent>();

  /**
   * Register of the eyebrow: `creation` (brand) when someone has to open something, `waiting`
   * (cyan) when the data arrives on its own — the same split `app-resource-state` draws.
   */
  public readonly tone = input<'creation' | 'waiting'>('creation');

  protected readonly readoutTones = READOUT_TONES;
}
