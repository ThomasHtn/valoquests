import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Page rendered for any URL that matches no route.
 *
 * Offers a way back to the overview rather than leaving the user on an empty shell.
 */
@Component({
  selector: 'app-not-found',
  imports: [TranslatePipe, RouterLink],
  templateUrl: './not-found.html',
})
export class NotFound {}
