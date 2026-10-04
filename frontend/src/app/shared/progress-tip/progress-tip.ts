import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { MarkDetail } from '@core/challenges/card/challenge-card.model';

/**
 * Progress tooltip: operator, exact figures, state and gap; the opener draws the gauge.
 */
@Component({
  selector: 'app-progress-tip',
  imports: [TranslatePipe, LucideCheck],
  templateUrl: './progress-tip.html',
  styleUrl: './progress-tip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[style.--tone]': 'detail().tone' },
})
export class ProgressTip {
  /**
   * Operator, figures and state the tooltip spells out.
   */
  public readonly detail = input.required<MarkDetail>();
}
