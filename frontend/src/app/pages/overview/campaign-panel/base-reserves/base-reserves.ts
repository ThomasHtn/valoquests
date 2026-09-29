import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideSkull, LucideUsers, LucideWheat, LucideWrench } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Reserves } from '../campaign-panel.model';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';

/**
 * The base's reserves: the two stocks and what they pay for, the wounded brought home since the
 * campaign opened, and what capped each settled Sunday.
 */
@Component({
  selector: 'app-base-reserves',
  imports: [InView, CountUp, TranslatePipe, LucideSkull, LucideUsers, LucideWheat, LucideWrench],
  templateUrl: './base-reserves.html',
  styleUrl: './base-reserves.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseReserves {
  public readonly reserves = input.required<Reserves>();

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected percent(fraction: number): string {
    return `${Math.round(fraction * 100)}%`;
  }
}
