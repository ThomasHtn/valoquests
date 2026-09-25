import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import {
  LucideBuilding2,
  LucideCrown,
  LucideFlame,
  LucideTarget,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideX,
} from '@lucide/angular';

import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { CountUp } from '@shared/count-up/count-up';
import { MissionReport as MissionReportView } from '../overview.model';
import { RESCUE_SEGMENT_COUNT } from './mission-report.constants';

/**
 * The Monday report: what Sunday settled, told as a game's end-of-mission screen over the overview.
 *
 * Three beats, revealed once in sequence: the verdict, the loot brought home, the honours. Opens on
 * its own the first time a settled week is seen, and again from the context bar's button.
 */
@Component({
  selector: 'app-mission-report',
  imports: [
    TranslatePipe,
    Avatar,
    CountUp,
    LucideBuilding2,
    LucideCrown,
    LucideFlame,
    LucideTarget,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideX,
  ],
  templateUrl: './mission-report.html',
  styleUrl: './mission-report.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'contents',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class MissionReport {
  public readonly report = input.required<MissionReportView>();

  public readonly open = input.required<boolean>();

  public readonly closed = output<void>();

  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  /**
   * The rescue gauge's segments, lit up to the share of wounded brought home.
   */
  protected readonly segments = computed(() => {
    const lit = Math.round(this.report().rescuedShare * RESCUE_SEGMENT_COUNT);
    return Array.from({ length: RESCUE_SEGMENT_COUNT }, (_, index) => index < lit);
  });

  /**
   * Why the rest of the wounded stayed behind: a stock ran short, or the breach fell short.
   */
  protected readonly leftBehindKey = computed(() => {
    const limiter = this.report().limiter;
    const cause = limiter === 'FOOD' || limiter === 'COMPONENTS' ? limiter : 'BREACH';
    return `overview.missionReport.leftBehind.${cause}`;
  });

  private readonly translation = inject(Translation);

  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  constructor() {
    effect(() => {
      if (this.open()) {
        this.panel()?.nativeElement.focus();
      }
    });
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}
