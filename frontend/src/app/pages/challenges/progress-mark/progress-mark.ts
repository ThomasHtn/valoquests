import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { InView } from '@shared/in-view/in-view';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardMark } from '../challenges.model';
import { ProgressTip } from '../progress-tip/progress-tip';
import { CONFETTI } from './progress-mark.constants';

/**
 * One operator's cell on the board: a dash before they start, the whole cell filling from the floor
 * with their figure while they progress, a check once they validate. Drawn in the row's `--tone`.
 */
@Component({
  selector: 'app-progress-mark',
  imports: [Tooltip, InView, ProgressTip, LucideCheck],
  templateUrl: './progress-mark.html',
  styleUrl: './progress-mark.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressMark {
  public readonly mark = input.required<BoardMark>();

  protected readonly confetti = CONFETTI;
}
