import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { ColonyView } from '@core/colony/colony-view';
import { ColonyTierStepView } from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * How many steps the strip shows at once.
 *
 * The ladder is twelve steps long and a citadel is drawn the same way a camp is, so showing all of
 * them costs width without saying more. Five is what it takes to read a position: two behind, the
 * one the colony stands on, and two still to climb.
 */
const WINDOW_SIZE = 5;

/**
 * One step as the strip draws it.
 */
export interface LadderStripStep {
  readonly name: string;

  /** Position on the full ladder, from one — what the current step's medallion carries. */
  readonly position: number;
  readonly reached: boolean;
  readonly current: boolean;

  /** The step being climbed, which is the only one the trail draws as still open. */
  readonly next: boolean;
}

/**
 * The ladder, read as the trail it is rather than as twelve rows.
 *
 * `/campaign` owns the full ladder with its thresholds; here the question is only "where are we, and
 * what is the next stop", so the strip carries a window around the current step and the materials
 * still missing. Clicking through goes to the campaign for the rest.
 */
@Component({
  selector: 'app-ladder-strip',
  imports: [TranslatePipe, DecimalPipe, LucideCheck],
  templateUrl: './ladder-strip.html',
  styleUrl: './ladder-strip.css',
})
export class LadderStrip {
  protected readonly colony = inject(ColonyView);

  /**
   * The step the colony is climbing towards, which the caption is priced in.
   */
  protected readonly next = computed<ColonyTierStepView | undefined>(() =>
    this.colony.ladder().find((tier) => tier.isNext),
  );

  /**
   * The window of steps around the colony's own, clamped to the ends of the ladder so the strip is
   * always the same width whether the colony is on its first step or its last.
   */
  protected readonly steps = computed<readonly LadderStripStep[]>(() => {
    const ladder = this.colony.ladder();
    if (ladder.length === 0) {
      return [];
    }

    const currentIndex = Math.max(
      0,
      ladder.findIndex((tier) => tier.state === 'CURRENT'),
    );
    const start = Math.max(0, Math.min(ladder.length - WINDOW_SIZE, currentIndex - 2));

    return ladder.slice(start, start + WINDOW_SIZE).map((tier, offset) => ({
      name: tier.name,
      position: start + offset + 1,
      reached: tier.state === 'REACHED',
      current: tier.state === 'CURRENT',
      next: tier.isNext,
    }));
  });

  /**
   * How far the colony has climbed towards the next step, `0`–`100`.
   */
  protected readonly progressPercentage = computed(() => this.next()?.progressPercentage ?? 0);
}
