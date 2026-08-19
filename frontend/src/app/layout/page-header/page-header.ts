import { Component, ElementRef, inject, input, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideMenu } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { NavigationPanel } from '@layout/navigation-panel';

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
 * Three inputs and two slots, because a page header is only ever these shapes:
 * - {@link eyebrow} names the context above the title — the active week, the section of the
 *   backoffice, or the page this one was reached from.
 * - {@link backLink} turns that same line into the way back to that parent.
 * - {@link heading} is dropped on a page whose subject is already named by the block right under
 *   the bar (the player profile opens on the portrait it belongs to), leaving the way back as the
 *   one thing the chrome carries; the eyebrow then renders as a control rather than a caption.
 * - `[headingAside]` sits on the title's own line, for a chip qualifying it.
 * - the default slot sits at the trailing edge, for what the page offers here: a countdown, a
 *   primary action, a view toggle.
 *
 * What deliberately stays out of the bar: controls that *govern the content* rather than the page
 * (the profile's game-mode, season and period filters, the campaign's legend). They belong beside
 * what they filter, and folding them in here would crowd a bar that has to survive a 360px screen.
 */
@Component({
  selector: 'app-page-header',
  imports: [RouterLink, TranslatePipe, LucideChevronLeft, LucideMenu],
  templateUrl: './page-header.html',
  // `shrink-0`: a flex item of `page-stack` (see the template) alongside the page's `page-body`,
  // which is the one that should give up height if the two ever compete for it.
  host: { class: 'block shrink-0' },
})
export class PageHeader {
  /**
   * Already-translated context line above the title: the section this page belongs to, or the page
   * it was reached from. The line is dropped when empty.
   */
  public readonly eyebrow = input('');

  /**
   * Already-translated page title, rendered as the page's `<h1>`.
   *
   * Left empty by a page that names its own subject in its opening block, which is then that
   * page's `<h1>`. The bar carries no title at all there rather than a weaker copy of it.
   */
  public readonly heading = input('');

  /**
   * Route the context line links back to, for a page reached from another one. When set,
   * {@link eyebrow} names that parent and becomes the way back to it.
   */
  public readonly backLink = input<string | null>(null);

  /**
   * Shared open state of the navigation drawer, which the burger below `lg` toggles.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * The burger itself, handed to {@link NavigationPanel.open} so closing the drawer returns focus
   * to it.
   */
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  /**
   * Opens the navigation drawer, remembering the burger as the control to focus on close.
   */
  protected openNavigation(): void {
    const trigger = this.menuButton()?.nativeElement;

    if (trigger) {
      this.navigationPanel.open(trigger);
    }
  }
}
