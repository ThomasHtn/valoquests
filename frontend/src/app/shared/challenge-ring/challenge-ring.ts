import { Component, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';

/**
 * A single player's progress toward one challenge, the shape {@link ChallengeRing} needs to draw
 * itself — deliberately narrower than `RankingCell` (`pages/leaderboard/leaderboard.model.ts`), so
 * this shared component does not depend on that page's own model. `RankingCell` structurally
 * satisfies it as-is.
 */
export interface ChallengeRingCell {
  readonly categoryLabel: string;
  readonly currentValueLabel: string;
  readonly targetValueLabel: string | null;
  readonly completionPercentage: number;
  readonly completed: boolean;
  readonly visual: {
    readonly iconClass: string;
    readonly badgeClass: string;
  };
}

/**
 * One cell of a challenge progress matrix: a filled badge once completed, a ring closing toward
 * it otherwise — the ranking table's own reading of "how far is this player on this challenge",
 * reused wherever else that same question comes up (the player profile's own "this week" band).
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
}
