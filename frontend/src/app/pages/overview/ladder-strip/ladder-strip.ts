import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LucideHammer } from '@lucide/angular';

import { ColonyView } from '@core/colony/colony-view';
import { ColonyTierGlyph, ColonyTierStepView } from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Breakpoint } from '@core/viewport/breakpoint';
import { TierGlyph } from '@shared/tier-glyph/tier-glyph';

/**
 * How many steps the strip shows on a phone.
 *
 * The wide layout draws every step the backend sent and never a fixed count: `ColonyQueryService`
 * already decided the window there — one step behind the colony's own and four ahead, so six, or
 * five while it still stands on the first — and a second window on top of that one silently dropped
 * the furthest step ahead, which is the one the strip exists to point at.
 *
 * A phone gets three: six names in 380 pixels is six truncated names, and the question a phone is
 * asked is only "where are we, and what is next".
 */
const NARROW_WINDOW = 3;

/**
 * Resolves where a window of {@link size} steps starts, centred on the colony's own step and clamped
 * to both ends of what the backend sent.
 *
 * Pure and exported for its own test: this is the second time the strip has been re-windowed, and the
 * first version quietly cut a step off the end.
 *
 * @param currentIndex - Index of the colony's own step in the ladder it was given.
 * @param length - How many steps the backend sent.
 * @param size - How many the strip has room for.
 * @returns Index the window starts at.
 */
export function ladderWindowStart(currentIndex: number, length: number, size: number): number {
  return Math.max(0, Math.min(length - size, currentIndex - Math.floor(size / 2)));
}

/**
 * One step as the strip draws it.
 */
export interface LadderStripStep {
  readonly name: string;

  /** Which silhouette the step's marker wears — the four bands `tierGlyphFor` sorts the ladder into. */
  readonly glyph: ColonyTierGlyph;
  readonly reached: boolean;
  readonly current: boolean;

  /** The step being climbed, which is the only one the trail draws as still open. */
  readonly next: boolean;

  /** What the step still costs, on the step being climbed and nowhere else. */
  readonly costLabel: string | null;
}

/**
 * The ladder, read as the trail it is rather than as twelve rows.
 *
 * `/campaign` owns the full ladder with its thresholds; here the question is only "where are we, and
 * what is the next stop", so the strip carries a window around the current step. Clicking through
 * goes to the campaign for the rest.
 *
 * Two things do the persuading. Each marker wears the silhouette of what that step actually builds,
 * the same four bands the scene at the top of the page grows through — a locked step is worth
 * wanting because its shape is visible. And the segment between the current step and the next *is*
 * the gauge: the percentage used to be a word at the end of a sentence, and it is now the distance
 * still to walk, drawn where the walking happens.
 */
@Component({
  selector: 'app-ladder-strip',
  imports: [TranslatePipe, DecimalPipe, LucideHammer, TierGlyph],
  templateUrl: './ladder-strip.html',
  styleUrl: './ladder-strip.css',
})
export class LadderStrip {
  protected readonly colony = inject(ColonyView);

  private readonly breakpoint = inject(Breakpoint);

  /**
   * The step the colony is climbing towards, which the trail's own segment is filled on.
   */
  protected readonly next = computed<ColonyTierStepView | undefined>(() =>
    this.colony.ladder().find((tier) => tier.isNext),
  );

  /**
   * The steps the strip draws: everything the backend sent on a wide screen, narrowed around the
   * colony's own step on a phone and clamped to both ends, so the strip is the same width whether the
   * colony is on the first step of the window or the last.
   */
  protected readonly steps = computed<readonly LadderStripStep[]>(() => {
    const ladder = this.colony.ladder();
    if (ladder.length === 0) {
      return [];
    }

    const start = this.windowStart();

    return ladder.slice(start, start + this.windowSize()).map((tier) => ({
      name: tier.name,
      glyph: tier.glyph,
      reached: tier.state === 'REACHED',
      current: tier.state === 'CURRENT',
      next: tier.isNext,
      costLabel: tier.isNext ? tier.missingMaterialsLabel : null,
    }));
  });

  /**
   * How far the colony has climbed towards the next step, `0`–`100`.
   */
  protected readonly progressPercentage = computed(() => this.next()?.progressPercentage ?? 0);

  private readonly windowSize = computed(() =>
    this.breakpoint.isMedium() ? this.colony.ladder().length : NARROW_WINDOW,
  );

  /**
   * First step of the window: the colony's own, less what fits behind it, clamped to both ends.
   */
  private readonly windowStart = computed(() => {
    const ladder = this.colony.ladder();
    const currentIndex = Math.max(
      0,
      ladder.findIndex((tier) => tier.state === 'CURRENT'),
    );

    return ladderWindowStart(currentIndex, ladder.length, this.windowSize());
  });
}
