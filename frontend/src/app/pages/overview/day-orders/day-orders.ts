import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import {
  LucideBuilding2,
  LucideSkull,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Countdown } from '@shared/countdown/countdown';
import { SectionRule } from '@shared/section-rule/section-rule';
import { Tooltip } from '@shared/tooltip/tooltip';
import { DailyOrder, DayTally } from '../overview.model';

/**
 * The orders of the day, in two equal columns: on the left what there is to do, on the right
 * what the day has already given.
 */
@Component({
  selector: 'app-day-orders',
  imports: [
    TranslatePipe,
    SectionRule,
    Countdown,
    Tooltip,
    LucideBuilding2,
    LucideSkull,
    LucideUserCheck,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideZap,
  ],
  templateUrl: './day-orders.html',
  styleUrl: './day-orders.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayOrders {
  /**
   * The day's challenge, or `null` when none was drawn.
   */
  public readonly order = input.required<DailyOrder | null>();

  /**
   * What the day has given, or `null` outside a week in progress.
   */
  public readonly tally = input.required<DayTally | null>();

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected signed(amount: number): string {
    const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
    return `${sign}${this.format(Math.abs(amount))}`;
  }

  /**
   * `Boss 04`, padded like the frieze's own week labels, rather than the guardian's own name.
   */
  protected bossLabel(weekIndex: number): string {
    return this.translation.translate('overview.report.boss', {
      index: String(weekIndex).padStart(2, '0'),
    });
  }

  /**
   * Names of the operators who validated the daily challenge, the mobile substitute for the
   * hover names the hexagon gauge used to carry.
   */
  protected doneTooltip(order: DailyOrder): string {
    const names = order.validated
      .filter((operator) => operator.done)
      .map((operator) => operator.name);
    if (names.length === 0) {
      return this.translation.translate('overview.orders.noneValidatedYet');
    }
    return this.translation.translate('overview.orders.validatedNames', {
      count: names.length,
      names: names.join(', '),
    });
  }
}
