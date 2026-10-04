import { Location, NgTemplateOutlet } from '@angular/common';
import { Component, computed, ElementRef, inject, input, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideMenu } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { NavigationHistory } from '@core/navigation/navigation-history';
import { resolveBackLabelKey } from '@core/navigation/navigation-history.utils';
import { NavigationPanel } from '@layout/navigation-panel/navigation-panel';
import { BackTarget } from './page-header.model';

/**
 * Context bar atop every page: a heading or a way back, plus page actions in the default slot.
 * Required on every shell page: below `lg` it carries the only burger (see `PAGE_LAYOUT_CLASS`).
 */
@Component({
  selector: 'app-page-header',
  imports: [NgTemplateOutlet, RouterLink, TranslatePipe, LucideChevronLeft, LucideMenu],
  templateUrl: './page-header.html',
  // `shrink-0`: `page-body` is the one that gives up height.
  host: { class: 'block shrink-0' },
})
export class PageHeader {
  /**
   * Translated `<h1>`, empty when the page's opening block names its subject.
   */
  public readonly heading = input('');

  /**
   * Fallback parent route when the previous page cannot be named (shared link, reload).
   */
  public readonly backLink = input<string | null>(null);

  /**
   * Translated name of the {@link backLink} page.
   */
  public readonly backLabel = input('');

  /**
   * Drawer state, opened by the burger.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * Burger, refocused when the drawer closes.
   */
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  /**
   * Navigation history, to name the page the back link returns to.
   */
  private readonly history = inject(NavigationHistory);

  /**
   * Browser location, to step back through history.
   */
  private readonly location = inject(Location);

  /**
   * Translation service, to name the previous page.
   */
  private readonly translation = inject(Translation);

  /**
   * The previous page when it can be named, the static parent otherwise.
   */
  protected readonly back = computed<BackTarget | null>(() => {
    const parent = this.backLink();
    if (parent === null) {
      return null;
    }
    const previous = this.history.previousUrl();
    const key = previous === null ? null : resolveBackLabelKey(previous);
    if (previous !== null && key !== null) {
      return { link: previous, label: this.translation.translate(key), viaHistory: true };
    }
    return { link: parent, label: this.backLabel(), viaHistory: false };
  });

  /**
   * Steps back through history; modified clicks keep the link's behaviour (new tab, window).
   */
  protected goBack(event: MouseEvent): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    event.preventDefault();
    this.location.back();
  }

  /**
   * Opens the drawer, remembering the burger to refocus on close.
   */
  protected openNavigation(): void {
    const trigger = this.menuButton()?.nativeElement;

    if (trigger) {
      this.navigationPanel.open(trigger);
    }
  }
}
