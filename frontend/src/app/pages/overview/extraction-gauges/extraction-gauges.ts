import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideBuilding2, LucideInfo, LucideDynamicIcon } from '@lucide/angular';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Tooltip } from '@shared/tooltip/tooltip';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import { Capacity } from './extraction-gauges.model';
import {
  CARRY_MODES,
  HULL_MASK,
  HULL_PATH,
  HULL_VIEWBOX,
  SHELTER_MODES,
} from './extraction-gauges.constants';
import { hullFigureSize } from './extraction-gauges.utils';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Sunday's extraction: three limiting dials, then what gets through, over the wounded spotted.
 * Each dial shows its raw stock, so the player can decide to save up.
 */
@Component({
  selector: 'app-extraction-gauges',
  imports: [
    LucideDynamicIcon,
    LowerCasePipe,
    TranslatePipe,
    LucideBuilding2,
    LucideInfo,
    Tooltip,
    CountUp,
    InView,
  ],
  templateUrl: './extraction-gauges.html',
  styleUrl: './extraction-gauges.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractionGauges {
  /**
   * Icon of each concept, for the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Four dials, `null` outside a week in progress.
   */
  public readonly capacity = input.required<Capacity | null>();

  /**
   * Week number, shown on the breakthrough dial as `Boss 04`.
   */
  public readonly weekIndex = input.required<number>();

  /**
   * Game modes listed under the carry dial as its main sources.
   */
  protected readonly carryModes = CARRY_MODES;

  /**
   * Game modes listed under the shelter dial as its main sources.
   */
  protected readonly shelterModes = SHELTER_MODES;

  /**
   * Frame of the aboard dial's rocket outline.
   */
  protected readonly hullViewBox = HULL_VIEWBOX;

  /**
   * Rocket outline drawn around the aboard dial.
   */
  protected readonly hullPath = HULL_PATH;

  /**
   * Rocket mask that keeps the aboard level inside the hull.
   */
  protected readonly hullMask = HULL_MASK;

  /**
   * Shrinks the aboard figure as its digits grow, to fit the hull.
   */
  protected readonly hullFigureSize = hullFigureSize;

  /**
   * Translation service, to word the tooltips and format figures.
   */
  private readonly translation = inject(Translation);

  /**
   * `Boss 04`, padded like the frieze's week labels.
   */
  protected readonly bossLabel = computed(() =>
    this.translation.translate('overview.report.boss', {
      index: String(this.weekIndex()).padStart(2, '0'),
    }),
  );

  /**
   * Info button text explaining how the carry dial is worked out.
   */
  protected readonly carryTooltip = computed(() =>
    this.translation.translate('overview.capacity.carryTooltip', {
      rate: this.capacity()?.componentsPerRescue ?? 0,
    }),
  );

  /**
   * Info button text explaining how the shelter dial is worked out.
   */
  protected readonly shelterTooltip = computed(() =>
    this.translation.translate('overview.capacity.shelterTooltip', {
      rate: this.capacity()?.foodPerRescue ?? 0,
    }),
  );

  /**
   * Info button text explaining the breakthrough dial.
   */
  protected readonly breachTooltip = computed(() =>
    this.translation.translate('overview.capacity.breachTooltip'),
  );

  /**
   * Info button text explaining who gets aboard on Sunday.
   */
  protected readonly aboardTooltip = computed(() =>
    this.translation.translate('overview.capacity.aboardTooltip'),
  );

  /**
   * Formats an amount in the active language for the dial figures.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * Turns a dial fraction into a whole percentage for its label.
   */
  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }
}
