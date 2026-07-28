import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Sidebar } from './layout/sidebar/sidebar';

/**
 * Root component.
 *
 * Renders the persistent sidebar alongside the routed page content.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
