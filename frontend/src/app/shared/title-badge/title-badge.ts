import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideFlame, LucideTarget, LucideWheat, LucideWrench } from '@lucide/angular';

import { WeeklyTitle } from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * A weekly title as it is worn everywhere: its icon, its word, its colour, and on hover what it
 * rewards. One fixed line high, so a titled row never stands taller than its neighbours.
 */
@Component({
  selector: 'app-title-badge',
  imports: [TranslatePipe, Tooltip, LucideFlame, LucideTarget, LucideWheat, LucideWrench],
  templateUrl: './title-badge.html',
  styleUrl: './title-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'contents' },
})
export class TitleBadge {
  public readonly title = input.required<WeeklyTitle>();

  /**
   * The figure the title was awarded on, already worded, appended to the hint.
   */
  public readonly measure = input<string | null>(null);

  /**
   * Icon size: `sm` in dense rows, `md` next to a heading.
   */
  public readonly size = input<'sm' | 'md'>('sm');

  protected readonly visual = computed(() => resolveTitleVisual(this.title()));
}
