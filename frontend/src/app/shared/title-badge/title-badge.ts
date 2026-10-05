import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { TitleKey } from '@core/campaign/titles/campaign-title.model';
import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Tooltip } from '@shared/tooltip/tooltip';
import { TITLE_BADGE_ICONS } from './title-badge.constants';

/**
 * A title with icon, word and tint, and on hover what it rewards.
 */
@Component({
  selector: 'app-title-badge',
  imports: [LucideDynamicIcon, TranslatePipe, Tooltip],
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
   * Translation service, to word the hint.
   */
  private readonly translation = inject(Translation);

  /**
   * Lucide icon of each title icon key.
   */
  protected readonly icons = TITLE_BADGE_ICONS;

  /**
   * Icon key and tone of the title.
   */
  protected readonly visual = computed(() => resolveTitleVisual(this.title()));

  /**
   * Tooltip: what the title rewards, then the figure it was awarded on.
   */
  protected readonly hint = computed(() => {
    const reward = this.translation.translate(`common.titleHint.${this.title()}`);
    const measure = this.measure();

    return measure ? `${reward} · ${measure}` : reward;
  });
}
