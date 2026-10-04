import { Component, input } from '@angular/core';
import {
  LucideActivity,
  LucideCalendar,
  LucideCrosshair,
  LucideShield,
  LucideSkull,
  LucideStar,
  LucideSwords,
  LucideTarget,
  LucideTrendingUp,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

import { ChallengeIcon } from '@core/challenges/visual/challenge-visual.model';

/**
 * Lucide icon of a {@link ChallengeIcon} key; size and color it with classes on the host.
 */
@Component({
  selector: 'app-challenge-icon-view',
  imports: [
    LucideActivity,
    LucideCalendar,
    LucideCrosshair,
    LucideShield,
    LucideSkull,
    LucideStar,
    LucideSwords,
    LucideTarget,
    LucideTrendingUp,
    LucideTrophy,
    LucideUsers,
  ],
  templateUrl: './challenge-icon-view.html',
  styleUrl: './challenge-icon-view.scss',
})
export class ChallengeIconView {
  /**
   * Icon key, resolved from the challenge's metric and difficulty.
   */
  public readonly icon = input.required<ChallengeIcon>();
}
