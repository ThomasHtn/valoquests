import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
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
  imports: [RouterOutlet, Sidebar, TranslatePipe],
  templateUrl: './shell.html',
  // `contents` so the shell element itself never becomes a box between `<app-root>` and the
  // full-height flex layout its template lays out.
  host: { class: 'contents' },
})
export class Shell {}
