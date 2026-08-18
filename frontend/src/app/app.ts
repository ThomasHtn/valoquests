import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Snackbar } from '@shared/snackbar/snackbar';

/**
 * Root component.
 *
 * Holds nothing but the top-level outlet and the snackbar: the chrome shared by the application's
 * pages lives in `Shell`, which the router activates as a layout route, so that the landing page
 * can render without it. The snackbar is mounted here instead, one level up, since it must also
 * cover the chrome-free admin sign-in screen that sits outside `Shell`.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Snackbar],
  templateUrl: './app.html',
})
export class App {}
