import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Page rendered for any URL that matches no route.
 *
 * Offers a way back to the overview rather than leaving the user on an empty shell.
 */
@Component({
  selector: 'app-not-found',
  imports: [TranslatePipe, RouterLink, PageHeader],
  templateUrl: './not-found.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class NotFound {}
