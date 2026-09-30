import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideCheck, LucideSkull, LucideSwords, LucideUsers } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CONFETTI } from '@pages/challenges/progress-mark/progress-mark.constants';
import { Countdown } from '@shared/countdown/countdown';
import { Tooltip } from '@shared/tooltip/tooltip';
import { CountUp } from '@shared/count-up/count-up';
import { InView } from '@shared/in-view/in-view';
import { Contribution, Mission, SundayStakes } from '../overview.model';
import { ContributionTip } from './contribution-tip/contribution-tip';
import { Strike } from './mission-readings.model';

/**
 * The week's mission: the clock, the squad against the guardian, then what Sunday midnight can
 * still add or take.
 *
 * The duel's whole ground is the track the two camps share: violet up to the breakthrough, cut into
 * one segment per operator and weighted by the damage each dealt, then the guardian's red. Once the
 * guardian is down, a check lands between the camps as a validated challenge's does, and the
 * figures and segments stay readable under it.
 *
 * The duel and the stakes carry a `data-card` anchor: `ScanWires` measures them to land its
 * callout wires from the planet beside them.
 */
@Component({
  selector: 'app-mission-readings',
  imports: [
    TranslatePipe,
    Countdown,
    CountUp,
    InView,
    Tooltip,
    ContributionTip,
    LucideCheck,
    LucideSkull,
    LucideSwords,
    LucideUsers,
  ],
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
   * What the squad has put into the week, `null` before its first match.
   */
  public readonly contribution = input.required<Contribution | null>();

  /**
   * Sunday's two outcomes, `null` once the guardian is down or while the forecast is unresolved.
   */
  public readonly stakes = input<SundayStakes | null>(null);

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
   * so they stay in the bubble rather than widening a segment.
   */
  protected readonly strikes = computed<readonly Strike[]>(() => {
    const hits = (this.contribution()?.shares ?? []).filter((share) => share.damage > 0);
    const dealt = hits.reduce((sum, share) => sum + share.damage, 0);
    return hits
      .map((share) => {
        const percent = Math.round((share.damage / dealt) * 100);
        const damageLabel = this.format(share.damage);
        return {
          playerId: share.playerId,
          name: share.name,
          damage: share.damage,
          damageLabel,
          percent,
          challengePoints: share.challengePoints,
          summary: this.translation.translate('overview.report.strike.summary', {
            name: share.name,
            damage: damageLabel,
            percent,
            points: share.challengePoints,
          }),
        };
      })
      .sort((left, right) => right.damage - left.damage);
  });

  /**
   * The check's burst, the challenges board's own pieces thrown further for the larger mark.
   */
  protected readonly confetti = CONFETTI;

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
