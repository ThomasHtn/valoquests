import { Component, inject } from '@angular/core';
import { LucideArrowUp } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageScroll } from '@core/scroll/page-scroll';

/**
 * The way back to the top of a long page, shown once the reader is more than a screen deep.
 *
 * The overview and the campaign run past four thousand pixels on a phone, where the context bar's
 * burger is the only thing within reach of the top and scrolling back is a dozen flicks.
 */
@Component({
  selector: 'app-scroll-top',
  imports: [LucideArrowUp, TranslatePipe],
  templateUrl: './scroll-top.html',
  host: { class: 'contents' },
})
export class ScrollTop {
  protected readonly pageScroll = inject(PageScroll);
}
