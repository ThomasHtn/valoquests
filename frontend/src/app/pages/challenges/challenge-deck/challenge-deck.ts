import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { LucideStar } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardOperator, BoardRow, DayCell } from '../challenges.model';
import { DailyWeek } from '../daily-week/daily-week';
import { DeckCard } from '../deck-card/deck-card';

/**
 * The board where a table would not fit: the squad bar to pin an operator first, then one card per
 * challenge, the day's first, with a line per operator closing toward the target.
 */
@Component({
  selector: 'app-challenge-deck',
  imports: [TranslatePipe, Tooltip, Avatar, DailyWeek, DeckCard, LucideStar],
  templateUrl: './challenge-deck.html',
  styleUrl: './challenge-deck.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChallengeDeck {
  /**
   * The squad, in board order.
   */
  public readonly operators = input.required<readonly BoardOperator[]>();

  /**
   * The challenges, the day's first.
   */
  public readonly rows = input.required<readonly BoardRow[]>();

  /**
   * The seven days of the week, for the day's tally.
   */
  public readonly days = input.required<readonly DayCell[]>();

  /**
   * Index of the day whose challenge the daily card shows.
   */
  public readonly pickedDay = model<number | null>(null);

  /**
   * Emits the operator tapped on the squad bar.
   */
  public readonly pin = output<number>();
}
