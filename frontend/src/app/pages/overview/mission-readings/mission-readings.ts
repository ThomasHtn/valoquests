import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideSwords, LucideUsers } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Countdown } from '@shared/countdown/countdown';
import { Tooltip } from '@shared/tooltip/tooltip';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import { Capacity, Contribution, Mission } from '../overview.model';

/**
 * The week's mission, in three readings: the clock, the squad against the guardian, what comes home.
 *
 * The squad and the guardian share one track, split at the breakthrough: the violet the squad has
 * taken on the left, the red still standing on the right, a cursor on the seam. The squad's side is cut into one segment
 * per operator, weighted by the damage each dealt, so the seam always sits on the real hit points.
 *
 * The fight and "aboard" rows carry a `data-card` anchor: `ScanWires` measures them to land its
 * callout wires from the planet beside them.
 */
@Component({
  selector: 'app-mission-readings',
  imports: [TranslatePipe, Countdown, CountUp, InView, Tooltip, LucideSwords, LucideUsers],
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
   * Whether to open on the week's clock. The tour drops it on a phone, where the two other
   * readings already fill the screen.
   */
  public readonly showClock = input(true);

  /**
   * The guardian's number, padded like the frieze's own week labels so `Boss 04` and the frieze's
   * `04` are read as the same thing.
   */
  protected readonly bossIndex = computed(() => String(this.mission().weekIndex).padStart(2, '0'));

  /**
   * Hit points taken from the guardian, capped at its pool like the figure on the other side.
   */
  protected readonly hitPointsDealt = computed(
    () => this.mission().hitPoints - this.mission().hitPointsLeft,
  );

  /**
   * Share of the track the squad holds, in [0, 1].
   */
  protected readonly breach = computed(() => 1 - this.mission().guardianLeft);

  /**
   * The operators who hit the guardian, heaviest first: challenge points never touch its hit points,
   * so they stay in the tooltip rather than widening a segment.
   */
  protected readonly strikes = computed(() =>
    (this.contribution()?.shares ?? [])
      .filter((share) => share.damage > 0)
      .sort((left, right) => right.damage - left.damage),
  );

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }
}
