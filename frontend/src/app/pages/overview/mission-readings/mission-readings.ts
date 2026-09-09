import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideSkull, LucideSwords, LucideUsers } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Countdown } from '@shared/countdown/countdown';
import { Tooltip } from '@shared/tooltip/tooltip';
import { Capacity, Contribution, Mission } from '../overview.model';

/**
 * The week's mission, in four readings: the clock, what comes home, the guardian, the squad.
 *
 * Deliberately not a report sheet with a header: each row is one figure, the scale it is read
 * against, and a single bar, all drawn on the same track so the four read as one instrument. The
 * squad's bar rides on the guardian's own hit points rather than on the squad's total, which is
 * what lets the empty end of every bar mean the same thing — what is left to do before Sunday.
 *
 * The "aboard" row carries a `data-card` anchor: `ScanWires` measures it to land its callout wire
 * from the planet beside it.
 */
@Component({
  selector: 'app-mission-readings',
  imports: [TranslatePipe, Countdown, Tooltip, LucideSkull, LucideSwords, LucideUsers],
  templateUrl: './mission-readings.html',
  styleUrl: './mission-readings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MissionReadings {
  /**
   * The week in progress.
   */
  public readonly mission = input.required<Mission>();

  /**
   * The extraction's four dials, `null` while the forecast is unresolved.
   */
  public readonly capacity = input.required<Capacity | null>();

  /**
   * What the squad has put into the week, `null` before its first match.
   */
  public readonly contribution = input.required<Contribution | null>();

  /**
   * The guardian's number, padded like the frieze's own week labels so `Boss 04` and the frieze's
   * `04` are read as the same thing.
   */
  protected readonly bossIndex = computed(() => String(this.mission().weekIndex).padStart(2, '0'));

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }
}
