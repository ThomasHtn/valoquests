import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Capacity } from '@pages/overview/extraction-gauges/extraction-gauges.model';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';

import { TourCapacityTile } from './tour-capacity.model';
import { buildCapacityTiles } from './tour-capacity.utils';

/**
 * The overview's two resource dials, kept side by side on a phone unlike `ExtractionGauges`.
 */
@Component({
  selector: 'app-tour-capacity',
  imports: [LowerCasePipe, TranslatePipe, CountUp, InView, LucideDynamicIcon],
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
   * The components dial and the food dial.
   */
  protected readonly tiles = computed<readonly TourCapacityTile[]>(() =>
    buildCapacityTiles(this.capacity()),
  );

  /**
   * Translation, to format the stocks in the current language.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats a stock in the current language.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }
}
