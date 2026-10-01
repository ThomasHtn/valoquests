import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { ChallengeHead } from '../challenge-head/challenge-head';
import { BoardRow } from '../challenges.model';
import { ProgressTip } from '../progress-tip/progress-tip';

/**
 * One challenge as a card: its compact head, the rule, then a line per operator closing toward the
 * target. The phone deck stacks them; the overview shows the day's. A caller can project a band
 * below the lines, the week's tally for one.
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
  public readonly row = input.required<BoardRow>();

  /**
   * Whether a line not started shows its figures (`0/3`) rather than a dash.
   */
  public readonly idleFigures = input(false);

  /**
   * Whether the card stands alone in a wide column: more padding, the rule of the challenge under
   * its name.
   */
  public readonly roomy = input(false);
}
