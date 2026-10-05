import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { InView } from '@shared/in-view/in-view';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardMark } from '@core/challenges/card/challenge-card.model';
import { ProgressTip } from '@shared/progress-tip/progress-tip';
import { CONFETTI } from './progress-mark.constants';

/**
 * Board cell of one operator: dash, progress ring, then a check, in the row's `--tone`.
 */
@Component({
  selector: 'app-progress-mark',
  imports: [Tooltip, InView, ProgressTip, LucideCheck],
  templateUrl: './progress-mark.html',
  styleUrl: './progress-mark.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressMark {
  /**
   * One operator's progress on the row's challenge.
   */
  public readonly mark = input.required<BoardMark>();

  /**
   * Burst pieces drawn when the mark turns into a check.
   */
  protected readonly confetti = CONFETTI;

  /**
   * Whether the figure needs the tighter type to fit the ring: a unit or more than two characters.
   */
  protected readonly hasLongFigure = computed(() => {
    const mark = this.mark();
    return mark.unit !== '' || mark.figure.length > 2;
  });
}
