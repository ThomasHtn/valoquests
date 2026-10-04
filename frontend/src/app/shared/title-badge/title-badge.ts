import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideCrown, LucideFlame, LucideTarget, LucideWheat, LucideWrench } from '@lucide/angular';

import { TitleKey } from '@core/campaign/titles/campaign-title.model';
import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * A title with icon, word and tint, and on hover what it rewards.
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
  /**
   * The title to show.
   */
  public readonly title = input.required<TitleKey>();

  /**
   * Worded figure the title was awarded on, appended to the hint.
   */
  public readonly measure = input<string | null>(null);

  /**
   * `sm` in dense rows, `md` next to a heading.
   */
  public readonly size = input<'sm' | 'md'>('sm');

  /**
   * Inside a link: no tab stop, as a focusable element nested in a link is invalid.
   */
  public readonly inLink = input(false);

  /**
   * Icon and tint of the title.
   */
  protected readonly visual = computed(() => resolveTitleVisual(this.title()));
}
