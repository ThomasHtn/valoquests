import {
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Tooltip } from '@shared/tooltip/tooltip';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, interval, map } from 'rxjs';
import {
  LucideBookOpen,
  LucideDatabaseBackup,
  LucideFlag,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideLogOut,
  LucideMap,
  LucideMenu,
  LucidePalette,
  LucideRefreshCw,
  LucideTarget,
  LucideTrophy,
  LucideUserCog,
  LucideUsers,
  LucideX,
} from '@lucide/angular';

import { AdminSession } from '@core/admin/admin-session';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Language } from '@core/i18n/translation.model';
import { PlayersApi } from '@core/players/players-api';
import { NavigationPanel } from '@layout/navigation-panel';
import { ADMIN_NAV_GROUPS, APP_VERSION, NAV_GROUPS } from './sidebar.constants';
import { NavItem } from './sidebar.model';
import { formatSynchronizationTimestamp, resolveLatestSynchronization } from './sidebar.utils';

/**
 * Persistent navigation sidebar.
 *
 * Displays the primary navigation, the last synchronization time and the language switch. Renders
 * as a vertical rail on `lg` and above, where it can be collapsed to icons only. Below that
 * breakpoint the same panel becomes a drawer sliding in from the left, so a phone keeps the full
 * labelled navigation instead of a truncated tab bar.
 *
 * The drawer's trigger is not here: below `lg` it is the burger of the routed page's context bar
 * (`layout/page-header/`), so the application shows one bar at the top of the page rather than a
 * navigation bar stacked over a page header. The open state the two share lives in
 * {@link NavigationPanel}.
 *
 * The host is `display: contents` so the `<aside>` is itself a flex item of the application shell,
 * sitting beside the routed content once the shell switches to a row on `lg`.
 */
@Component({
  selector: 'app-sidebar',
  host: {
    class: 'contents',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown)': 'onDocumentKeydown($event)',
  },
  imports: [
    RouterLink,
    LucideBookOpen,
    LucideDatabaseBackup,
    LucideFlag,
    LucideLanguages,
    LucideLayoutDashboard,
    LucideLogOut,
    LucideMap,
    LucideMenu,
    LucidePalette,
    LucideRefreshCw,
    LucideTarget,
    LucideTrophy,
    LucideUserCog,
    LucideUsers,
    LucideX,
    TranslatePipe,
    Tooltip,
  ],
  templateUrl: './sidebar.html',
})
export class Sidebar {
  /**
   * Data-access service backing the shared players resource, used to resolve the latest
   * synchronization timestamp shown in the footer.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to translate the footer's loading, error and unknown fallback labels.
   */
  private readonly translation = inject(Translation);

  /**
   * Router used to track the active route for {@link isNavItemActive}.
   */
  private readonly router = inject(Router);

  /**
   * Backoffice session, which decides which set of navigation entries the rail offers.
   */
  private readonly adminSession = inject(AdminSession);

  /**
   * Shared open state of the drawer, whose trigger lives in the routed page's context bar.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * URL of the currently active route, refreshed on every navigation.
   *
   * Backs {@link isNavItemActive}: a nav entry can stay highlighted across more than the one route
   * its own `routerLink` points to (see `NavItem.activeRoutes`), which the declarative
   * `routerLinkActive` directive cannot express on its own, so active-state matching is done here
   * instead.
   */
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /**
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

  /**
   * Whether a backoffice session is open.
   *
   * Also gates the sign-out control in the footer, which is the only way back out of the
   * backoffice: nothing in the application links into it, so nothing links out of it either.
   */
  protected readonly adminMode = computed(() => this.adminSession.isAuthenticated());

  /**
   * Navigation chapters currently on offer.
   *
   * The backoffice replaces the public entries rather than adding to them — see
   * {@link ADMIN_NAV_GROUPS}.
   */
  protected readonly navGroups = computed(() => (this.adminMode() ? ADMIN_NAV_GROUPS : NAV_GROUPS));

  /**
   * Version shown at the very bottom of the sidebar.
   */
  protected readonly appVersion = APP_VERSION;

  /**
   * Languages the switcher offers, in display order.
   */
  protected readonly supportedLanguages = this.translation.supportedLanguages;

  /**
   * Currently active language, marking the pressed button of the switcher.
   */
  protected readonly language = this.translation.language;

  /**
   * Width utility applied to the rail, reflecting {@link collapsed}.
   *
   * Resolved here rather than through `[class.lg:w-20]` bindings because Angular class bindings
   * cannot express a Tailwind variant prefix. Only applies from `lg` up, since below that
   * breakpoint the panel is a fixed-width drawer.
   */
  protected readonly railWidthClass = computed(() => (this.collapsed() ? 'lg:w-20' : 'lg:w-64'));

  /**
   * Cursor utility applied to the whole rail while collapsed, signalling that clicking any empty
   * area re-expands it — the same affordance ChatGPT's collapsed sidebar uses. Only meaningful on
   * the rail: buttons and links inside it declare their own cursor and take precedence over this
   * inherited value, so it only ever shows over genuinely empty space. Rail-only for the same
   * reason as {@link brandBlockClass}: collapsing never applies to the drawer.
   */
  protected readonly railCursorClass = computed(() =>
    this.collapsed() ? 'lg:cursor-ew-resize' : '',
  );

  /**
   * Position and visibility utilities driving the drawer below `lg`, reflecting
   * {@link NavigationPanel.isOpen}. The rail restores both from `lg` up through static `lg:`
   * utilities.
   *
   * `invisible` rather than `hidden`: it keeps the panel out of the tab order and out of the
   * accessibility tree while closed, without taking it out of the layout mid-slide.
   *
   * Each state carries its own transition rather than sharing one declared on the element, because
   * `visibility` has to be timed in opposite directions. Riding the shared 300ms transition, it
   * only resolves to `visible` once the transition has actually started — two frames after the
   * class lands — so the panel is still unfocusable at the moment the drawer moves focus into it,
   * and a keyboard user opening the menu was left on the document body. It therefore flips at once
   * on the way in (`visibility 0s`), and is held back until the slide has finished on the way out
   * (`visibility 0s 300ms`), which is what keeps the panel on screen for the whole exit.
   */
  protected readonly drawerClass = computed(() =>
    this.navigationPanel.isOpen()
      ? 'visible translate-x-0 [transition:translate_300ms_ease-out,visibility_0s]'
      : 'invisible -translate-x-full [transition:translate_300ms_ease-out,visibility_0s_300ms]',
  );

  /**
   * Opacity and visibility utilities driving the scrim behind the drawer, for the same reason as
   * {@link drawerClass}.
   */
  protected readonly scrimClass = computed(() =>
    this.navigationPanel.isOpen() ? 'visible opacity-100' : 'invisible opacity-0',
  );

  /**
   * Display utility applied to the wordmark block, which the collapsed rail replaces with its "V"
   * mark. Only hidden from `lg` up: the drawer always shows the full wordmark, since collapsing is
   * a rail-only affordance and its state must not leak into the mobile presentation.
   */
  protected readonly brandBlockClass = computed(() => (this.collapsed() ? 'lg:hidden' : 'lg:flex'));

  /**
   * Display utility applied to the last-synchronization block, hidden on a collapsed rail in favour
   * of its icon-only counterpart. Rail-only for the same reason as {@link brandBlockClass}.
   */
  protected readonly syncBlockClass = computed(() => (this.collapsed() ? 'lg:hidden' : 'lg:block'));

  /**
   * Display utility applied to the version line, hidden on a collapsed rail: at `lg:w-20` there is
   * no room for the string, and it is the least essential thing in the footer. Rail-only for the
   * same reason as {@link brandBlockClass}.
   */
  protected readonly versionClass = computed(() => (this.collapsed() ? 'lg:hidden' : 'lg:block'));

  /**
   * Alignment utility applied to each navigation entry on the rail: centered once collapsed, so
   * the icon sits in the middle of the icon-only rail, and leading otherwise.
   *
   * Both sides are resolved here rather than pairing a static `lg:justify-start` with a bound
   * `lg:justify-center`, since two utilities of equal specificity would be settled by their order
   * in the stylesheet instead of by the collapsed state. Never applies to the tab bar, whose
   * entries are always centered.
   */
  protected readonly navItemClass = computed(() =>
    this.collapsed() ? 'lg:justify-center' : 'lg:justify-start',
  );

  /**
   * Visibility utility applied to each navigation label, hiding it on a collapsed rail.
   *
   * The label stays in the DOM rather than behind an `@if` so the tab bar, which always shows it,
   * shares the same markup.
   */
  protected readonly navLabelClass = computed(() => (this.collapsed() ? 'lg:hidden' : ''));

  /**
   * Visibility utility applied to each chapter caption, hidden on a collapsed rail where a
   * hairline marks the break instead.
   */
  protected readonly navGroupLabelClass = computed(() => (this.collapsed() ? 'lg:hidden' : ''));

  /**
   * Visibility utility applied to the hairline between chapters, shown only on a collapsed rail.
   */
  protected readonly navGroupRuleClass = computed(() => (this.collapsed() ? 'lg:block' : ''));

  /**
   * Direction/alignment utility applied to the row pairing the last-synchronization info with the
   * language switcher: it stacks and centers on a collapsed rail, too narrow to hold both side by
   * side, and stays a left/right row everywhere else.
   *
   * Resolved here for the same reason as {@link navItemClass}: a Tailwind variant prefix cannot be
   * expressed through an Angular class binding, and pairing a static `lg:` utility with a bound one
   * for the same property would leave their precedence to stylesheet generation order rather than
   * the collapsed state.
   */
  protected readonly footerContentClass = computed(() =>
    this.collapsed() ? 'lg:flex-col lg:items-center' : 'lg:flex-row lg:justify-between',
  );

  /**
   * Size/alignment/state utility applied to the language switcher trigger: a centered icon-only
   * square on a collapsed rail, its natural icon+code width everywhere else, plus the trigger's own
   * active state once its panel is open — the same tint used on hover, with gold text layered on
   * top so the open state reads as a stronger version of the hover state rather than a distinct one.
   */
  protected readonly languageButtonClass = computed(() => {
    const sizeClass = this.collapsed() ? 'lg:w-9 lg:justify-center' : '';
    const stateClass = this.languageMenuOpen() ? 'bg-brand-500/8 text-brand-500' : '';
    return `${sizeClass} ${stateClass}`;
  });

  /**
   * Visibility utility hiding the language code next to the trigger's icon on a collapsed rail,
   * where there is no room for it.
   */
  protected readonly languageCodeClass = computed(() => (this.collapsed() ? 'lg:hidden' : ''));

  /**
   * Position utility applied to the language switcher's panel: left-aligned to its icon-only
   * trigger on a collapsed rail, opening into the routed content rather than centered — the rail is
   * too narrow at `lg:w-20` for a centered panel to fit without spilling past the viewport's left
   * edge. Right-aligned to the trigger everywhere else, which does have the room.
   */
  protected readonly languagePanelClass = computed(() =>
    this.collapsed() ? 'lg:right-auto lg:left-0' : '',
  );

  /**
   * Whether the language switcher's panel is open.
   */
  protected readonly languageMenuOpen = signal(false);

  /**
   * Id of the language switcher's panel, referenced by the trigger's `aria-controls`.
   */
  protected readonly languageMenuId = 'sidebar-language-menu';

  /**
   * Host element of the language switcher (trigger + panel), used to detect clicks landing outside
   * it so the panel closes without needing a backdrop.
   */
  private readonly languageMenuElement = viewChild<ElementRef<HTMLElement>>('languageMenu');

  /**
   * Drawer's close button, focused when the drawer opens so keyboard and screen-reader users land
   * inside the panel they just summoned.
   */
  private readonly closeMenuButton = viewChild<ElementRef<HTMLButtonElement>>('closeMenuButton');

  /**
   * Reactive resource fetching every tracked player's synchronization status.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Timestamp of the last player synchronization, shown in the footer.
   *
   * Resolves to a translated loading, error or unknown fallback while the backing resource is not
   * ready or no player has been synchronized successfully yet.
   */
  protected readonly lastSyncLabel = computed(() => {
    if (this.playersResource.isLoading()) {
      return this.translation.translate('sidebar.lastSync.loading');
    }

    if (this.playersResource.error()) {
      return this.translation.translate('sidebar.lastSync.error');
    }

    const latest = resolveLatestSynchronization(this.playersResource.value() ?? []);
    return latest
      ? formatSynchronizationTimestamp(latest)
      : this.translation.translate('sidebar.lastSync.unknown');
  });

  /**
   * Availability of the backend API, inferred from the shared players resource used to resolve
   * {@link lastSyncLabel}.
   */
  protected readonly apiStatus = computed<'online' | 'offline'>(() =>
    this.playersResource.error() ? 'offline' : 'online',
  );

  /**
   * Translated label describing the current {@link apiStatus}, used as the status dot's
   * accessible name and tooltip.
   */
  protected readonly apiStatusLabel = computed(() =>
    this.translation.translate(`sidebar.lastSync.status.${this.apiStatus()}`),
  );

  constructor() {
    // Moves focus into the drawer as it opens, so keyboard and screen-reader users land inside the
    // panel they just summoned rather than back at the top of the document.
    //
    // An after-render effect rather than a plain one: while closed the panel is
    // `visibility: hidden`, which makes its controls unfocusable, and the class driving that only
    // lands once the state change has been rendered. It reacts to the shared state rather than
    // sitting in an open handler, since the control that opens the drawer belongs to the routed
    // page's context bar, not to this component.
    afterRenderEffect(() => {
      if (this.navigationPanel.isOpen()) {
        this.closeMenuButton()?.nativeElement.focus();
      }
    });

    // The players resource is fetched once and never re-requested on its own, so `lastSyncLabel`
    // would otherwise freeze at whatever it read on load even as the backend keeps synchronizing
    // every 30 minutes. The sidebar is mounted for the app's whole lifetime, so it is a fitting
    // place to keep this shared resource current.
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.playersResource.reload());
  }

  /**
   * Whether `item` should render as the active navigation entry.
   *
   * `exactMatch` entries only match the current URL outright; every other entry also matches a
   * child route under its own `routerLink`, and under any of its `activeRoutes` (a second page
   * reached from within the section rather than from the sidebar, which still shares this one
   * entry).
   *
   * @param item - The navigation entry to check.
   * @returns Whether the entry is active for the current route.
   */
  protected isNavItemActive(item: NavItem): boolean {
    const url = this.currentUrl();
    const routes = [item.routerLink, ...(item.activeRoutes ?? [])].filter(
      (route): route is string => !!route,
    );

    return routes.some(
      (route) => url === route || (!item.exactMatch && url.startsWith(`${route}/`)),
    );
  }

  /**
   * Utilities layered onto an active entry on top of {@link navItemClass}, empty otherwise.
   *
   * @param item - The navigation entry to check.
   * @returns The active-state utilities, or the empty string.
   */
  protected navActiveClass(item: NavItem): string {
    return this.isNavItemActive(item)
      ? ' bg-linear-to-r from-brand-500/20 to-transparent text-brand-500 before:bg-brand-500'
      : '';
  }

  /**
   * Toggles the sidebar between its expanded and icon-only collapsed state.
   */
  protected toggleCollapsed(): void {
    this.collapsed.update((collapsed) => !collapsed);
  }

  /**
   * Expands the rail when a click lands on empty space while it is collapsed, mirroring
   * {@link railCursorClass}'s affordance.
   *
   * Ignores clicks landing inside a button or link — those already carry their own behaviour (the
   * collapse toggle, navigation) and must not also re-expand the rail from underneath them.
   *
   * @param event - The click event bubbling up from the rail.
   */
  protected onRailClick(event: MouseEvent): void {
    if (!this.collapsed()) {
      return;
    }

    if ((event.target as HTMLElement).closest('button, a')) {
      return;
    }

    this.toggleCollapsed();
  }

  /**
   * Closes the drawer, returning focus to the control that opened it.
   *
   * Safe to call from the rail too, where there is no drawer to close: the shared state guards on
   * its own open state.
   */
  protected closeMobileMenu(): void {
    this.navigationPanel.close();
  }

  /**
   * Dismisses the drawer once a navigation entry has been activated, and hands focus to the page
   * that entry just opened.
   *
   * Focus cannot go back to the burger the way it does on a plain dismissal: the routed page owns
   * it, so the one that opened the drawer is destroyed by this very navigation. The routed content
   * is the right landing point anyway — it is what the visitor asked for, and it is already the
   * skip link's target, so it is focusable. It is also the shell's own element rather than the
   * page's, so it outlives the navigation and can be focused straight away.
   */
  protected onNavItemActivated(): void {
    if (!this.navigationPanel.isOpen()) {
      return;
    }

    this.navigationPanel.close();
    document.getElementById('main-content')?.focus();
  }

  /**
   * Switches the application to `language` and closes the switcher's panel.
   *
   * The returned promise is deliberately not awaited: the switch is already reflected by the
   * `language` signal the moment it is set, and the dictionary it then loads swaps in on its own
   * through {@link TranslatePipe}. Failures are handled inside the service, which falls back to
   * rendering raw keys rather than rejecting.
   *
   * @param language - The language to switch to.
   */
  protected switchLanguage(language: Language): void {
    void this.translation.setLanguage(language);
    this.languageMenuOpen.set(false);
  }

  /**
   * Opens or closes the language switcher's panel.
   */
  protected toggleLanguageMenu(): void {
    this.languageMenuOpen.update((open) => !open);
  }

  /**
   * Closes the backoffice session and returns to the public application.
   *
   * Navigating away is part of the action rather than left to the visitor: the pages the session
   * was on are guarded, so staying on one would only bounce back to the sign-in screen.
   */
  protected signOutOfAdmin(): void {
    this.adminSession.signOut();
    this.closeMobileMenu();
    void this.router.navigate(['/overview']);
  }

  /**
   * Closes the language switcher's panel when a click lands outside it.
   *
   * Bound to the whole document rather than a host listener since the switcher is one control
   * among several in this component, not the whole component.
   *
   * @param event - The document-wide click event.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.languageMenuOpen()) {
      return;
    }

    const host = this.languageMenuElement()?.nativeElement;
    if (host && !host.contains(event.target as Node)) {
      this.languageMenuOpen.set(false);
    }
  }

  /**
   * Dismisses the innermost open layer on Escape, matching the ARIA disclosure pattern: the
   * language panel first, since it is stacked on top of the drawer, then the drawer itself.
   *
   * @param event - The document-wide keydown event.
   */
  protected onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') {
      return;
    }

    if (this.languageMenuOpen()) {
      this.languageMenuOpen.set(false);
      return;
    }

    this.closeMobileMenu();
  }
}
