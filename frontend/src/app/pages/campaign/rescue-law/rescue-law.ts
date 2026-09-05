import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import {
  LucideBuilding2,
  LucideGauge,
  LucideHeartPulse,
  LucideRocket,
  LucideSkull,
  LucideSwords,
  LucideTarget,
  LucideTrendingUp,
  LucideWheat,
  LucideWrench,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LawNotes, RescueLaw } from '../campaign.model';

/**
 * The rule of Sunday's settlement: a formula with the week's own figures in every term, and
 * three notes. This is where the game is explained; the overview only measures.
 */
@Component({
  selector: 'app-rescue-law',
  imports: [
    TranslatePipe,
    LucideBuilding2,
    LucideGauge,
    LucideHeartPulse,
    LucideRocket,
    LucideSkull,
    LucideSwords,
    LucideTarget,
    LucideTrendingUp,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './rescue-law.html',
  styleUrl: './rescue-law.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RescueLawView {
  /**
   * The formula's terms, or `null` outside a week in progress.
   */
  public readonly law = input.required<RescueLaw | null>();

  /**
   * The notes, or `null` without a campaign.
   */
  public readonly notes = input.required<LawNotes | null>();

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
