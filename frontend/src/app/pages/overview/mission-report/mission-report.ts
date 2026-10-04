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
  LucideCrown,
  LucideFileText,
  LucideFlame,
  LucideTarget,
  LucideX,
  LucideDynamicIcon,
} from '@lucide/angular';

import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { CountUp } from '@shared/count-up/count-up';
import { MissionReport as MissionReportView } from './mission-report.model';
import { RESCUE_SEGMENT_COUNT } from './mission-report.constants';
import { FocusTrap } from '@shared/focus-trap/focus-trap';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Monday report dialog: verdict, loot and honours of what Sunday settled.
 */
@Component({
  selector: 'app-mission-report',
  imports: [
    LucideDynamicIcon,
    FocusTrap,
    TranslatePipe,
    Avatar,
    Button,
    CountUp,
    LucideCrown,
    LucideFileText,
    LucideFlame,
    LucideTarget,
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
  /**
   * Icon of each concept, for the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Settled week the dialog reports on.
   */
  public readonly report = input.required<MissionReportView>();

  /**
   * Whether the dialog is shown.
   */
  public readonly open = input.required<boolean>();

  /**
   * Emitted when the player dismisses the dialog.
   */
  public readonly closed = output<void>();

  /**
   * Campaign length, for the "week 4 of 10" heading.
   */
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  /**
   * Rescue gauge segments, lit up to the share of wounded brought home.
   */
  protected readonly segments = computed(() => {
    const lit = Math.round(this.report().rescuedShare * RESCUE_SEGMENT_COUNT);
    return Array.from({ length: RESCUE_SEGMENT_COUNT }, (_, index) => index < lit);
  });

  /**
   * Why the other wounded stayed behind: a stock or the breach fell short.
   */
  protected readonly leftBehindKey = computed(() => {
    const limiter = this.report().limiter;
    const cause = limiter === 'FOOD' || limiter === 'COMPONENTS' ? limiter : 'BREACH';
    return `overview.missionReport.leftBehind.${cause}`;
  });

  /**
   * Active language, to format the report figures.
   */
  private readonly translation = inject(Translation);

  /**
   * Dialog panel, focused on opening so keyboard users land inside it.
   */
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  constructor() {
    effect(() => {
      if (this.open()) {
        this.panel()?.nativeElement.focus();
      }
    });
  }

  /**
   * Formats an amount in the active language for the loot figures.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * Closes the dialog on Escape, only while it is open.
   */
  protected onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}
