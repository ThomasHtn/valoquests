import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideUsers, LucideZap } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '../challenges.model';

/**
 * What a challenge is, as the board's first column and a phone card both open on it: the hexagon,
 * the key line (a closed day said on a daily), the name, the gain per operator, the rule.
 */
@Component({
  selector: 'app-challenge-head',
  imports: [TranslatePipe, Tooltip, LucideUsers, LucideZap],
  templateUrl: './challenge-head.html',
  styleUrl: './challenge-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChallengeHead {
  public readonly row = input.required<BoardRow>();
}
