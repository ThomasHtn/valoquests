import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LiveRefresh } from '@core/http/live-refresh';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { NavigationHistory } from '@core/navigation/navigation-history';
import { Breakpoint } from '@core/viewport/breakpoint';
import { NavigationPanel } from '@layout/navigation-panel';
import { PageScroll } from '@core/scroll/page-scroll';
import { RouteProgress } from '@layout/route-progress/route-progress';
import { ScrollTop } from '@layout/scroll-top/scroll-top';
import { Sidebar } from '@layout/sidebar/sidebar';

/**
 * Application shell.
 *
 * Layout route wrapping every page that belongs to the application proper: it owns the skip link
 * and the persistent sidebar, and renders the routed page beside them. The landing page sits
 * outside this shell — it is a full-bleed doorway whose only affordance is its own call to action,
 * so it must not inherit the navigation chrome.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouteProgress, ScrollTop, Sidebar, TranslatePipe],
  templateUrl: './shell.html',
  // `contents` so the shell element itself never becomes a box between `<app-root>` and the
  // full-height flex layout its template lays out.
  host: { class: 'contents' },
})
export class Shell {
  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');

  protected readonly navigationPanel = inject(NavigationPanel);

  protected readonly breakpoint = inject(Breakpoint);

  constructor() {
    // Started here rather than in a page: the shell outlives every route, so the screens keep
    // following the backend for as long as the tab is open.
    inject(LiveRefresh);
    // Eager for the same reason: it must hear every page body's scroll to restore it on back.
    inject(PageScroll);
    // Same: a back link can only name the page before if every navigation was heard.
    inject(NavigationHistory);
  }

  /**
   * Moves focus to the routed content. Handled here because `<base href="/">` resolves the bare
   * `#main-content` against the root, which would reload the landing page instead.
   */
  protected skipToContent(event: Event): void {
    event.preventDefault();
    this.main().nativeElement.focus();
  }
}
