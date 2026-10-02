import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideCrown, LucideFlame, LucideTarget, LucideWheat, LucideWrench } from '@lucide/angular';

import { TitleKey } from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * A title as it is worn everywhere, the Champion's included: its icon, its word, its colour on a
 * flat tint of it, and on hover what it rewards. One fixed line high, so a titled row never stands
 * taller than its neighbours.
 */
@Component({
  selector: 'app-title-badge',
  imports: [
    TranslatePipe,
    Tooltip,
    LucideCrown,
    LucideFlame,
    LucideTarget,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './title-badge.html',
  styleUrl: './title-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'contents' },
})
export class TitleBadge {
  public readonly title = input.required<TitleKey>();

  /**
   * The figure the title was awarded on, already worded, appended to the hint.
   */
  public readonly measure = input<string | null>(null);

  /**
   * Icon size: `sm` in dense rows, `md` next to a heading.
   */
  public readonly size = input<'sm' | 'md'>('sm');

  /**
   * Whether the badge sits inside a link: it then takes no tab stop of its own, since a focusable
   * element nested in a link is invalid and a tap on it would follow the link anyway.
   */
  public readonly inLink = input(false);

  protected readonly visual = computed(() => resolveTitleVisual(this.title()));
}
