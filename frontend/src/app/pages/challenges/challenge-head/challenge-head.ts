import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideUsers, LucideZap } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '../challenges.model';
import { DailyClock } from '../daily-clock/daily-clock';

/**
 * What a challenge is, as the board's first column and a phone card both open on it: the hexagon,
 * the key line (with the day's countdown on a daily), the name, the gain per operator, the rule.
 */
@Component({
  selector: 'app-challenge-head',
  imports: [TranslatePipe, Tooltip, DailyClock, LucideUsers, LucideZap],
  templateUrl: './challenge-head.html',
  styleUrl: './challenge-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChallengeHead {
  public readonly row = input.required<BoardRow>();

  /**
   * Whether a daily shows its countdown on the key line; off where the caller draws it elsewhere.
   */
  public readonly clock = input(true);
}
