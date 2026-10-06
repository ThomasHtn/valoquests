import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LucideDynamicIcon, LucideInfo } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import { Tooltip } from '@shared/tooltip/tooltip';

import { HULL_MASK, HULL_PATH, HULL_VIEWBOX } from './extraction-gauges.constants';
import { Capacity, LimitDial } from './extraction-gauges.model';
import { buildLimitDials, hullFigureSize } from './extraction-gauges.utils';

/**
 * Sunday's extraction: three limiting dials, then what gets through, over the wounded spotted.
 * Each dial shows its raw stock, so the player can decide to save up.
 */
@Component({
  selector: 'app-extraction-gauges',
  imports: [
    LucideDynamicIcon,
    NgTemplateOutlet,
    TranslatePipe,
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
   * Carry, shelter and breakthrough dials, empty outside a week in progress.
   */
  protected readonly dials = computed<readonly LimitDial[]>(() => {
    const capacity = this.capacity();
    if (!capacity) {
      return [];
    }
    return buildLimitDials(
      capacity,
      this.bossLabel(),
      (key, params) => this.translation.translate(key, params),
      (amount) => this.format(amount),
    );
  });

  /**
   * Accessible reading of the aboard dial, empty outside a week in progress.
   */
  protected readonly aboardLabel = computed(() => {
    const capacity = this.capacity();
    if (!capacity) {
      return '';
    }
    return this.translation.translate('overview.capacity.aboardAria', {
      count: this.format(capacity.aboard),
      wounded: this.format(capacity.wounded),
      percent: Math.round(capacity.aboardFraction * 100),
      extraction: this.format(capacity.fromGuardian),
      challenges: this.format(capacity.fromChallenges),
    });
  });

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
    return formatFigure(amount, this.translation.language());
  }
}
