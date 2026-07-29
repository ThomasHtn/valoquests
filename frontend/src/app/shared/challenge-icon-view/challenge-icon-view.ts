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

import { ChallengeIcon } from '@core/challenges/challenge-visual.model';

/**
 * Renders the Lucide icon matching a challenge's resolved {@link ChallengeIcon} key.
 *
 * Shared by the weekly challenges card and the weekly ranking table so both widgets render the
 * exact same icon for a given challenge. Consumers size and color the icon with Tailwind utility
 * classes (e.g. `h-5 w-5`, `text-accent-green`) applied directly on the host element.
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
  styleUrl: './challenge-icon-view.css',
})
export class ChallengeIconView {
  /**
   * Icon key to render, resolved from a challenge's metric and difficulty.
   */
  public readonly icon = input.required<ChallengeIcon>();
}
