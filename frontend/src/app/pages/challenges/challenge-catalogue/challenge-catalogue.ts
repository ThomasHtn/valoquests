import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideZap } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
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
}
