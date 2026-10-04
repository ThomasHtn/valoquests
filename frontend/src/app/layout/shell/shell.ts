import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LiveRefresh } from '@core/http/live-refresh/live-refresh';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { NavigationHistory } from '@core/navigation/navigation-history';
import { Breakpoint } from '@core/viewport/breakpoint';
import { NavigationPanel } from '@layout/navigation-panel/navigation-panel';
import { PageScroll } from '@core/scroll/page-scroll';
import { RouteProgress } from '@layout/route-progress/route-progress';
import { ScrollTop } from '@layout/scroll-top/scroll-top';
import { Sidebar } from '@layout/sidebar/sidebar';

/**
 * Layout route with the skip link and sidebar; the landing page stays outside it.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouteProgress, ScrollTop, Sidebar, TranslatePipe],
  templateUrl: './shell.html',
  // `contents` keeps the host out of the full-height flex layout.
  host: { class: 'contents' },
})
export class Shell {
  /**
   * Main content region, focused by the skip link.
   */
  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');

  /**
   * Drawer state, used by the template to react to the open drawer.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * Viewport breakpoints, to pick the layout in the template.
   */
  protected readonly breakpoint = inject(Breakpoint);

  constructor() {
    // Eager: the shell outlives every route, so these hear every navigation.
    inject(LiveRefresh);
    inject(PageScroll);
    inject(NavigationHistory);
  }

  /**
   * Focuses by hand: `<base href="/">` would resolve `#main-content` to the landing.
   */
  protected skipToContent(event: Event): void {
    event.preventDefault();
    this.main().nativeElement.focus();
  }
}
