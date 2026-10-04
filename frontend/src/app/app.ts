import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Snackbar } from '@shared/snackbar/snackbar';

/**
 * Root component; the snackbar sits here to also cover chrome-free screens outside `Shell`.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Snackbar],
  templateUrl: './app.html',
})
export class App {}
