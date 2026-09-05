import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideTarget, LucideUsers, LucideZap } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ChallengeCard } from '../challenges.model';

/**
 * One challenge: the hexagon, the key line, the name and what it asks, what it brings back per
 * operator, then the rule and the squad gauge.
 *
 * Shared by the day's drawer and the week's five, so both read as the same object.
 */
@Component({
  selector: 'app-challenge-card',
  imports: [TranslatePipe, LucideTarget, LucideUsers, LucideZap],
  templateUrl: './challenge-card.html',
  styleUrl: './challenge-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.card--done]': 'allDone()',
    '[style.--tone]': 'card().tone',
  },
})
export class ChallengeCardView {
  public readonly card = input.required<ChallengeCard>();

  /**
   * Whether every operator validated it: the card then turns green rather than lighting a full
   * row of hexagons alone.
   */
  protected readonly allDone = computed(() => {
    const { slots, doneCount } = this.card();
    return slots.length > 0 && doneCount === slots.length;
  });
}
