import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LucideCheck, LucideDynamicIcon } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { CountUp } from '@shared/count-up/count-up';
import { Countdown } from '@shared/countdown/countdown';
import { InView } from '@shared/in-view/in-view';
import { CONFETTI } from '@shared/progress-mark/progress-mark.constants';
import { Tooltip } from '@shared/tooltip/tooltip';

import { ContributionTip } from './contribution-tip/contribution-tip';
import { FallForecast } from './fall-forecast/fall-forecast';
import { GuardianFall } from './fall-forecast/fall-forecast.model';
import { Contribution, Mission, Strike, SundayStakes } from './mission-readings.model';

/**
 * The week's mission: clock, squad-versus-guardian duel, Sunday stakes and the guardian's fall.
 * The duel and the stakes carry `data-card` anchors that `ScanWires` measures.
 */
@Component({
  selector: 'app-mission-readings',
  imports: [
    LucideDynamicIcon,
    TranslatePipe,
    Countdown,
    CountUp,
    InView,
    Tooltip,
    ContributionTip,
    FallForecast,
    LucideCheck,
  ],
  templateUrl: './mission-readings.html',
  styleUrl: './mission-readings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MissionReadings {
  /**
   * Icon of each concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * The week in progress.
   */
  public readonly mission = input.required<Mission>();

  /**
   * Squad contribution, `null` before the first match.
   */
  public readonly contribution = input.required<Contribution | null>();

  /**
   * Sunday's two outcomes, `null` once the guardian is down or without a forecast.
   */
  public readonly stakes = input<SundayStakes | null>(null);

  /**
   * The guardian's descent and projection, `null` before the first hit.
   */
  public readonly fall = input<GuardianFall | null>(null);

  /**
   * Whether to show the week's clock; the tour hides it on a phone.
   */
  public readonly showClock = input(true);

  /**
   * Guardian number, padded like the frieze's week labels.
   */
  protected readonly bossIndex = computed(() => String(this.mission().weekIndex).padStart(2, '0'));

  /**
   * Hit points taken from the guardian.
   */
  protected readonly hitPointsDealt = computed(
    () => this.mission().hitPoints - this.mission().hitPointsLeft,
  );

  /**
   * Share of the track the squad holds, in [0, 1].
   */
  protected readonly breach = computed(() => 1 - this.mission().guardianLeft);

  /**
   * The squad's share as a CSS length, for the track's `--breach`.
   */
  protected readonly breachLevel = computed(() => `${this.breach() * 100}%`);

  /**
   * Operators who hit the guardian, heaviest first; challenge points never widen a segment.
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
   * Confetti burst of the defeat check.
   */
  protected readonly confetti = CONFETTI;

  /**
   * Translation service, to format damage and word the strike summaries.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the strikes and stakes.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }
}
