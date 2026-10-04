import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Strike } from '../mission-readings.model';

/**
 * Operator bubble on the duel track, laid out like the challenges board's progress bubble.
 */
@Component({
  selector: 'app-contribution-tip',
  imports: [TranslatePipe],
  templateUrl: './contribution-tip.html',
  styleUrl: './contribution-tip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContributionTip {
  /**
   * Operator's share of the week's damage, shown in the bubble.
   */
  public readonly strike = input.required<Strike>();
}
