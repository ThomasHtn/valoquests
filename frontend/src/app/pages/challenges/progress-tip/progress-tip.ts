import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { MarkDetail } from '../challenges.model';

/**
 * The content of a progress bubble: operator, exact figures over the target, a gauge that runs
 * past the target when it is exceeded, then the state and what remains or exceeds.
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
  public readonly detail = input.required<MarkDetail>();
}
