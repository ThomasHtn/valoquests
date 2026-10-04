import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { ChallengeHead } from '@shared/challenge-head/challenge-head';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { ProgressTip } from '@shared/progress-tip/progress-tip';

/**
 * Challenge card: compact head, one progress line per operator, projected content below.
 */
@Component({
  selector: 'app-deck-card',
  imports: [TranslatePipe, Tooltip, Avatar, ChallengeHead, ProgressTip, LucideCheck],
  templateUrl: './deck-card.html',
  styleUrl: './deck-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'article',
    '[attr.aria-label]': 'row().name',
    '[class.deck-card--roomy]': 'roomy()',
    '[style.--tone]': 'row().tone',
  },
})
export class DeckCard {
  /**
   * The challenge the card shows, with its progress lines.
   */
  public readonly row = input.required<BoardRow>();

  /**
   * Idle lines show their figures (`0/3`) instead of a dash.
   */
  public readonly idleFigures = input(false);

  /**
   * Alone in a wide column: more padding, rule aligned under the name.
   */
  public readonly roomy = input(false);
}
