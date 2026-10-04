import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Reserves } from '../campaign-panel.model';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

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
   * Concept icons for the template.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Stocks, rescue totals and Sunday limits the panel lays out.
   */
  public readonly reserves = input.required<Reserves>();

  /**
   * Active language, to format figures with its separators.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the tanks and legends.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * Turns a fill fraction into the CSS level of a tank bar.
   */
  protected percent(fraction: number): string {
    return `${Math.round(fraction * 100)}%`;
  }
}
