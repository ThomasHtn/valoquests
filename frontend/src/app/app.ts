import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root component.
 *
 * Holds nothing but the top-level outlet: the chrome shared by the application's pages lives in
 * `Shell`, which the router activates as a layout route, so that the landing page can render
 * without it.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
})
export class App {}
