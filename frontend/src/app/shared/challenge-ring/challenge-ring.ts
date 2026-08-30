import { Component, computed, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';

/**
 * What the ring holds, in characters, measured against the 34px disc it encloses at `size-11`:
 * `text-2xs` runs ~6px per monospaced digit and `text-3xs tracking-tighter` ~5px, and a grouped
 * damage count spends a character on its thousands separator too. Past the wider of the two, no
 * type step left is still legible — a six-figure total is abbreviated instead.
 */
const LABEL_FITS_AT_BASE_SIZE = 4;
const LABEL_FITS_IN_RING = 6;

/**
 * A single player's progress toward one challenge, the shape {@link ChallengeRing} needs to draw
 * itself — deliberately narrower than `RankingCell` (`pages/leaderboard/leaderboard.model.ts`), so
 * this shared component does not depend on that page's own model. `RankingCell` structurally
 * satisfies it as-is.
 */
export interface ChallengeRingCell {
  readonly categoryLabel: string;
  readonly currentValueLabel: string;
  readonly compactValueLabel: string;
  readonly targetValueLabel: string | null;
  readonly completionPercentage: number;
  readonly completed: boolean;
  readonly visual: {
    readonly iconClass: string;
    readonly badgeClass: string;
  };
}

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
