import { Component, inject } from '@angular/core';
import { LucideArrowUp } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageScroll } from '@core/scroll/page-scroll';

/**
 * Back-to-top button, shown once the reader is more than a screen deep.
 */
@Component({
  selector: 'app-scroll-top',
  imports: [LucideArrowUp, TranslatePipe],
  templateUrl: './scroll-top.html',
  styleUrl: './scroll-top.scss',
  host: { class: 'contents' },
})
export class ScrollTop {
  /**
   * Page scroll state, telling when to show the button and scrolling to the top.
   */
  protected readonly pageScroll = inject(PageScroll);
}
