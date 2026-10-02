import { Location, NgTemplateOutlet } from '@angular/common';
import { Component, computed, ElementRef, inject, input, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideMenu } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { NavigationHistory } from '@core/navigation/navigation-history';
import { resolveBackLabelKey } from '@core/navigation/navigation-history.utils';
import { NavigationPanel } from '@layout/navigation-panel';
import { BackTarget } from './page-header.model';

/**
 * The application's context bar: one compact row pinned to the top of the routed content, on every
 * page and at every breakpoint.
 *
 * It is chrome, not a block of the page. Rendered by the page (so a title can be a translated
 * string, a week number or a player's name without any of it travelling through a store) as the
 * first item of `page-stack`, outside the scroll container the rest of the page's content sits in
 * (`page-body`, see `styles.css`) — so it stays put without needing to be `sticky`, and the body's
 * scrollbar never runs behind it. Spans the content column edge to edge and closed by the
 * direction's gold rule. Together with the sidebar's rail — same `surface-sunken` ground, same
 * rule — it frames the page in an L rather than competing with it: the rail answers "which
 * section", this bar answers "which page, and what can I do here".
 *
 * Below `lg` it also carries the burger, since the rail is a drawer there and the sidebar no longer
 * contributes a bar of its own. **Every page nested under the shell must therefore render this
 * component**, or a phone loses its way into the navigation (see `PAGE_LAYOUT_CLASS`).
 *
 * A page header is only ever one of two shapes:
 * - a {@link heading}: one short gold title stating what the page is, centred in the bar's height.
 * - a way back ({@link backLink} + {@link backLabel}) on a page whose subject is already named by
 *   the block right under the bar (the player profile opens on its portrait). `[backAside]` sits
 *   inline after it, for a discreet complement (the player's name).
 *
 * The default slot sits at the trailing edge, for what the page offers here: a countdown, a
 * primary action, a view toggle.
 *
 * What deliberately stays out of the bar: controls that *govern the content* rather than the page
 * (the profile's game-mode, season and period filters, the campaign's legend). They belong beside
 * what they filter, and folding them in here would crowd a bar that has to survive a 360px screen.
 */
@Component({
  selector: 'app-page-header',
  imports: [NgTemplateOutlet, RouterLink, TranslatePipe, LucideChevronLeft, LucideMenu],
  templateUrl: './page-header.html',
  // `shrink-0`: a flex item of `page-stack` (see the template) alongside the page's `page-body`,
  // which is the one that should give up height if the two ever compete for it.
  host: { class: 'block shrink-0' },
})
export class PageHeader {
  /**
   * Already-translated page title, rendered as the page's `<h1>`. Left empty by a page that names
   * its own subject in its opening block, which is then that page's `<h1>`.
   */
  public readonly heading = input('');

  /**
   * Parent route the bar leads back to when the reader did not arrive from a page it can name
   * (a shared link, a reload). Otherwise the way back is the page they came from.
   */
  public readonly backLink = input<string | null>(null);

  /**
   * Already-translated name of the parent page {@link backLink} leads to.
   */
  public readonly backLabel = input('');

  /**
   * Shared open state of the navigation drawer, which the burger below `lg` toggles.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * The burger itself, handed to {@link NavigationPanel.open} so closing the drawer returns focus
   * to it.
   */
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  private readonly history = inject(NavigationHistory);

  private readonly location = inject(Location);

  private readonly translation = inject(Translation);

  /**
   * The way back: the page the reader came from when it can be named, the static parent otherwise.
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
   * Opens the navigation drawer, remembering the burger as the control to focus on close.
   */
  protected goBack(event: MouseEvent): void {
    // Modified clicks keep the link's own behaviour (new tab, new window).
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    event.preventDefault();
    this.location.back();
  }

  protected openNavigation(): void {
    const trigger = this.menuButton()?.nativeElement;

    if (trigger) {
      this.navigationPanel.open(trigger);
    }
  }
}
