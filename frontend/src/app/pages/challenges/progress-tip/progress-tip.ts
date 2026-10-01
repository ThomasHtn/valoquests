import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { MarkDetail } from '../challenges.model';

/**
 * The content of a progress bubble: operator, exact figures over the target, then the state and
 * what remains or exceeds. No gauge: the ring or line it opens from already draws one.
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
