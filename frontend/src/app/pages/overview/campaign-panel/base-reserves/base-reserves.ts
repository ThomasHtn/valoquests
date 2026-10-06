import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';

import { Reserves } from '../campaign-panel.model';
import { TANK_KEYS } from './base-reserves.constants';
import { RescueShare, SundayLimit } from './base-reserves.model';

/**
 * Base stocks, wounded brought home, and what capped each settled Sunday.
 */
@Component({
  selector: 'app-base-reserves',
  imports: [LucideDynamicIcon, InView, CountUp, TranslatePipe],
  templateUrl: './base-reserves.html',
  styleUrl: './base-reserves.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseReserves {
  /**
   * Stocks, rescue totals and Sunday limits the panel lays out.
   */
  public readonly reserves = input.required<Reserves>();

  /**
   * Concept icons for the template.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Stocks drawn as tanks.
   */
  protected readonly tankKeys = TANK_KEYS;

  /**
   * The rescue bar's three segments, which also make its legend.
   */
  protected readonly rescueShares = computed<readonly RescueShare[]>(() => {
    const reserves = this.reserves();
    const [extraction, challenges, leftBehind] = reserves.shares;
    return [
      {
        tone: 'extraction',
        labelKey: 'campaign.reserves.byExtraction',
        percent: extraction,
        count: reserves.byExtraction,
      },
      {
        tone: 'challenges',
        labelKey: 'campaign.reserves.byChallenges',
        percent: challenges,
        count: reserves.byChallenges,
      },
      {
        tone: 'left-behind',
        labelKey: 'campaign.reserves.leftBehind',
        percent: leftBehind,
        count: reserves.leftBehind,
      },
    ];
  });

  /**
   * What capped the settled Sundays: food, components, or nothing.
   */
  protected readonly sundayLimits = computed<readonly SundayLimit[]>(() => {
    const reserves = this.reserves();
    return [
      {
        tone: 'food',
        icon: CONCEPT_ICONS.food,
        count: reserves.limitedByFood,
        labelKey: 'campaign.reserves.limitFood',
      },
      {
        tone: 'components',
        icon: CONCEPT_ICONS.components,
        count: reserves.limitedByComponents,
        labelKey: 'campaign.reserves.limitComponents',
      },
      {
        tone: 'group',
        icon: CONCEPT_ICONS.wounded,
        count: reserves.wholeGroup,
        labelKey: 'campaign.reserves.limitGroup',
      },
    ];
  });

  /**
   * Active language, to format figures with its separators.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the tanks and legends.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }

  /**
   * Turns a fill fraction into the CSS level of a tank bar.
   */
  protected percent(fraction: number): string {
    return `${Math.round(fraction * 100)}%`;
  }
}
