import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { LABEL_FITS_AT_BASE_SIZE, LABEL_FITS_IN_RING } from './challenge-ring.constants';
import { ChallengeRingCell } from './challenge-ring.model';

/**
 * One cell of a challenge progress matrix: a ring closing clockwise toward its target, and once
 * closed a tinted disc inside it — the ranking table's own reading of "how far is this player on
 * this challenge", reused wherever else that same question comes up (the player profile's own
 * "this week" band).
 *
 * Completion keeps the ring rather than replacing it with a filled badge: the arc at full strength
 * against a centre at a fraction of it reads as *closed* from across the matrix, where a solid
 * badge only read as *different*.
 */
@Component({
  selector: 'app-challenge-ring',
  imports: [TranslatePipe, ProgressCircle],
  templateUrl: './challenge-ring.html',
})
export class ChallengeRing {
  /**
   * The progress this ring draws.
   */
  public readonly cell = input.required<ChallengeRingCell>();

  /**
   * How far round the arc travels. Completion closes it outright, rather than trusting the
   * percentage: a challenge can be marked completed on a rule the current value no longer
   * satisfies, and a ring left a few degrees short there would contradict its own tinted centre.
   */
  public readonly ringPercentage = computed(() =>
    this.cell().completed ? 100 : this.cell().completionPercentage,
  );

  /**
   * The value as the ring shows it: the exact figure, or its abbreviation once the exact one runs
   * wider than the ring. The exact figure is still what the cell announces, so nothing is lost —
   * only the reading inside a 44px disc changes.
   */
  public readonly displayLabel = computed(() => {
    const cell = this.cell();
    return cell.currentValueLabel.length > LABEL_FITS_IN_RING
      ? cell.compactValueLabel
      : cell.currentValueLabel;
  });

  /**
   * Everything the value is set in: the challenge's own color, and the type scale step, stepped
   * down so the longer counts stay clear of the ring instead of running under it.
   *
   * One entry per class, never `'text-3xs tracking-tighter'` in a single one: Angular's array class
   * binding treats each entry as one name and silently drops any that holds a space, which left the
   * stepped-down labels with no size at all — at the inherited 16px, straight over the ring.
   */
  public readonly labelClasses = computed(() => {
    const step =
      this.displayLabel().length <= LABEL_FITS_AT_BASE_SIZE
        ? ['text-2xs']
        : ['text-3xs', 'tracking-tighter'];
    return [this.cell().visual.iconClass, ...step];
  });
}
