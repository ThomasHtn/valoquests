import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Strike } from '../mission-readings.model';

/**
 * The content of an operator's bubble on the duel track, laid out like the challenges board's
 * progress bubble: operator, damage, then their share of the squad and their challenge points.
 */
@Component({
  selector: 'app-contribution-tip',
  imports: [TranslatePipe],
  templateUrl: './contribution-tip.html',
  styleUrl: './contribution-tip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContributionTip {
  public readonly strike = input.required<Strike>();
}
