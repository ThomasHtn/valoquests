import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Sidebar } from '@layout/sidebar/sidebar';

/**
 * Root component.
 *
 * Renders the skip link and the persistent sidebar alongside the routed page content.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, TranslatePipe],
  templateUrl: './app.html',
})
export class App {}
