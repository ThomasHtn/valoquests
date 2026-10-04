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
import { Capacity } from '@pages/overview/extraction-gauges/extraction-gauges.model';

/**
 * The overview's two resource dials, kept side by side on a phone unlike `ExtractionGauges`.
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

  /**
   * Game modes listed under the components dial.
   */
  protected readonly carryModes = CARRY_MODES;

  /**
   * Game modes listed under the food dial.
   */
  protected readonly shelterModes = SHELTER_MODES;

  /**
   * Translation, to format the stocks in the current language.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats a stock in the current language.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
