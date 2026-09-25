import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideLayoutDashboard } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { Button } from '@shared/button/button';
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import { EmptyPlate as EmptyPlateContent } from '@shared/empty-plate/empty-plate.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Page rendered for any URL that matches no route.
 *
 * Names the address that failed and offers a way back to the overview rather than leaving the user
 * on an empty shell.
 */
@Component({
  selector: 'app-not-found',
  imports: [TranslatePipe, RouterLink, PageHeader, Button, EmptyPlate, LucideLayoutDashboard],
  templateUrl: './not-found.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class NotFound {
  private readonly translation = inject(Translation);
  private readonly router = inject(Router);

  protected readonly plate = computed<EmptyPlateContent>(() => ({
    illustration: 'radar',
    eyebrow: this.translation.translate('notFound.eyebrow'),
    title: this.translation.translate('notFound.heading'),
    text: this.translation.translate('notFound.description'),
    readouts: [
      {
        tone: 'info',
        label: this.translation.translate('notFound.address'),
        value: this.router.url,
      },
    ],
  }));
}
