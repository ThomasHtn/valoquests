import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import {
  LucideBuilding2,
  LucideSkull,
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
    LucideBuilding2,
    LucideSkull,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideZap,
  ],
  templateUrl: './day-orders.html',
  styleUrl: './day-orders.css',
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
}
