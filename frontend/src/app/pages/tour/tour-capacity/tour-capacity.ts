import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideBuilding2, LucideRocket, LucideWheat, LucideWrench } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import {
  CARRY_MODES,
  SHELTER_MODES,
} from '@pages/overview/extraction-gauges/extraction-gauges.constants';
import { Capacity } from '@pages/overview/overview.model';

/**
 * The two resource tiles of the overview's extraction capacity, and nothing else: components carry
 * the wounded, food shelters them, and each tile names the modes that fill it.
 *
 * Drawn after `ExtractionGauges` rather than reusing it: the tour needs the two stock dials side by
 * side on a phone, where the overview stacks all four.
 */
@Component({
  selector: 'app-tour-capacity',
  imports: [
    LowerCasePipe,
    TranslatePipe,
    CountUp,
    InView,
    LucideBuilding2,
    LucideRocket,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './tour-capacity.html',
  styleUrl: './tour-capacity.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourCapacity {
  /**
   * The dials to read the two stocks from.
   */
  public readonly capacity = input.required<Capacity>();

  protected readonly carryModes = CARRY_MODES;

  protected readonly shelterModes = SHELTER_MODES;

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
