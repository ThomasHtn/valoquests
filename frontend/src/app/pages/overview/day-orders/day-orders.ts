import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LucideDynamicIcon, LucideUserCheck } from '@lucide/angular';

import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Countdown } from '@shared/countdown/countdown';
import { DeckCard } from '@shared/deck-card/deck-card';
import { Tooltip } from '@shared/tooltip/tooltip';

import { DayTally, TallyTile } from './day-orders.model';
import { buildTallyTiles } from './day-orders.utils';

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
   * Day's base flows, empty outside a week in progress.
   */
  protected readonly tiles = computed<readonly TallyTile[]>(() => {
    const tally = this.tally();
    if (!tally) {
      return [];
    }
    return buildTallyTiles(
      tally,
      (key, params) => this.translation.translate(key, params),
      (amount) => this.format(amount),
    );
  });

  /**
   * Formats an amount in the active language for the tally tiles.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
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
