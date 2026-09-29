import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import {
  LucideBuilding2,
  LucideSkull,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
  LucideWrench,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Countdown } from '@shared/countdown/countdown';
import { Tooltip } from '@shared/tooltip/tooltip';
import { ChallengeCardView } from '../../challenges/challenge-card/challenge-card';
import { DailyOrder, DayTally } from '../overview.model';

/**
 * The orders of the day, in two equal columns: on the left the day's challenge card, on the right
 * what the day has already given.
 */
@Component({
  selector: 'app-day-orders',
  imports: [
    TranslatePipe,
    Countdown,
    Tooltip,
    LucideBuilding2,
    LucideSkull,
    LucideUserCheck,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    ChallengeCardView,
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
}
