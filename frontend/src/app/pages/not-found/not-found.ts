import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideLayoutDashboard } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { Button } from '@shared/button/button';
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import { EmptyPlate as EmptyPlateContent } from '@shared/empty-plate/empty-plate.model';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';

/**
 * Page for any unmatched URL: names the failed address and links back to the overview.
 */
@Component({
  selector: 'app-not-found',
  imports: [TranslatePipe, RouterLink, PageHeader, Button, EmptyPlate, LucideLayoutDashboard],
  templateUrl: './not-found.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class NotFound {
  /**
   * Translation service, to word the plate.
   */
  private readonly translation = inject(Translation);

  /**
   * Router, whose URL names the address that failed.
   */
  private readonly router = inject(Router);

  /**
   * Empty plate content: what went wrong and the unmatched address.
   */
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
