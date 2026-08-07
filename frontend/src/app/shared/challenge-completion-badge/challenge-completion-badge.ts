import { Component, computed, input } from '@angular/core';
import { LucideTarget } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Weekly challenge completion rendered as a target icon next to an `x/total` count, in the same
 * outline-pill treatment as `DamageBadge` so the two ranking stats read as one system.
 *
 * Shared by the ranking page's podium, table and mobile list so a player's completion reads the
 * same everywhere it appears.
 */
@Component({
  selector: 'app-challenge-completion-badge',
  imports: [TranslatePipe, LucideTarget],
  template: `
    <svg aria-hidden="true" class="h-3 w-3" lucideTarget></svg>
    {{ completed() }}/{{ total() }}
    <span class="sr-only">{{ 'shared.challengeCompletionBadge.label' | translate }}</span>
  `,
  host: {
    class:
      'inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs font-bold tabular-nums',
    '[class]': 'toneClass()',
  },
})
export class ChallengeCompletionBadge {
  /**
   * Number of challenges completed so far.
   */
  public readonly completed = input.required<number>();

  /**
   * Total number of challenges selected for the week.
   */
  public readonly total = input.required<number>();

  /**
   * Colors matching progress, mirroring `DamageBadge.toneClass`: neutral at zero, green as soon
   * as at least one challenge is completed.
   */
  protected readonly toneClass = computed(() =>
    this.completed() === 0
      ? 'border-surface-700 text-text-muted'
      : 'border-accent-green/40 text-accent-green',
  );
}
