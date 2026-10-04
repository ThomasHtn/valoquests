import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideUserCheck, LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Countdown } from '@shared/countdown/countdown';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { DeckCard } from '@shared/deck-card/deck-card';
import { DayTally } from './day-orders.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Orders of the day: the daily challenge card beside the day's haul.
 */
@Component({
  selector: 'app-day-orders',
  imports: [
    LucideDynamicIcon,
    TranslatePipe,
    Avatar,
    Countdown,
    Tooltip,
    LucideUserCheck,
    DeckCard,
  ],
  templateUrl: './day-orders.html',
  styleUrl: './day-orders.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayOrders {
  /**
   * Icon of each concept, for the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Day's challenge, `null` when none was drawn.
   */
  public readonly daily = input.required<BoardRow | null>();

  /**
   * Day's gains, `null` outside a week in progress.
   */
  public readonly tally = input.required<DayTally | null>();

  /**
   * Translation service, to format figures and word the boss label.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the tally tiles.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * Formats a gain or loss with its sign, a true minus for losses.
   */
  protected signed(amount: number): string {
    const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
    return `${sign}${this.format(Math.abs(amount))}`;
  }

  /**
   * `Boss 04`, padded like the frieze's week labels.
   */
  protected bossLabel(weekIndex: number): string {
    return this.translation.translate('overview.report.boss', {
      index: String(weekIndex).padStart(2, '0'),
    });
  }
}
