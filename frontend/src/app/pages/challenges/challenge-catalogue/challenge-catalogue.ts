import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideZap } from '@lucide/angular';

import { formatChallengeTarget } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CatalogueGroup } from '../challenges.model';

/**
 * Everything the draws can still hand out, grouped by what fixes the reward: the daily pool,
 * then the five difficulties.
 */
@Component({
  selector: 'app-challenge-catalogue',
  imports: [TranslatePipe, LucideZap],
  templateUrl: './challenge-catalogue.html',
  styleUrl: './challenge-catalogue.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChallengeCatalogueView {
  public readonly groups = input.required<readonly CatalogueGroup[]>();

  private readonly translation = inject(Translation);

  /**
   * The target as resolved for the campaign in force, the figure the description does not carry.
   */
  protected target(value: number): string {
    return formatChallengeTarget(value, this.translation.language());
  }
}
