import {
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild,
  DestroyRef,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Tooltip } from '@shared/tooltip/tooltip';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map } from 'rxjs';
import {
  LucideBookOpen,
  LucideDatabaseBackup,
  LucideFlag,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideLogOut,
  LucideMenu,
  LucidePalette,
  LucideRefreshCw,
  LucideTarget,
  LucideTrophy,
  LucideUserCog,
  LucideUsers,
  LucideX,
} from '@lucide/angular';

import { AdminSession } from '@core/admin/session/admin-session';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Language } from '@core/i18n/translation.model';
import { resourceValue } from '@core/http/resource-state.utils';
import { SynchronizationApi } from '@core/synchronization/synchronization-api';
import { NavigationPanel } from '@layout/navigation-panel/navigation-panel';
import {
  LANGUAGE_MENU_OPEN_CLASS,
  NAV_ACTIVE_CLASS,
  ADMIN_NAV_GROUPS,
  NAV_GROUPS,
  SIDEBAR_CLOCK_MS,
} from './sidebar.constants';
import { resolveDrawerClasses, resolveRailClasses } from './sidebar-classes.utils';
import { NavItem } from './sidebar.model';
import { formatElapsed } from '@core/date/date-format.utils';
import {
  formatSynchronizationTimestamp,
  isNavItemActive,
  isSynchronizationStale,
} from './sidebar.utils';
import { FocusTrap } from '@shared/focus-trap/focus-trap';

/**
 * Navigation rail from `lg` up (collapsible), drawer below it opened by the page header's burger.
 * The open state lives in {@link NavigationPanel}.
 */
@Component({
  selector: 'app-sidebar',
  host: {
    class: 'contents',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown)': 'onDocumentKeydown($event)',
  },
  imports: [
    FocusTrap,
    RouterLink,
    LucideBookOpen,
    LucideDatabaseBackup,
    LucideFlag,
    LucideLanguages,
    LucideLayoutDashboard,
    LucideLogOut,
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
   * Synchronization API, whose status feeds the last synchronization readout.
   */
  private readonly synchronizationApi = inject(SynchronizationApi);

  /**
   * Translation service, for the labels and the language switcher.
   */
  private readonly translation = inject(Translation);

  /**
   * Router, to track the current URL and leave the backoffice on sign-out.
   */
  private readonly router = inject(Router);

  /**
   * Backoffice session, to switch the navigation and sign out.
   */
  private readonly adminSession = inject(AdminSession);

  /**
   * Drawer state, shared with the page header's burger.
   */
  protected readonly navigationPanel = inject(NavigationPanel);

  /**
   * Current URL, matched by hand since `routerLinkActive` cannot express `activeRoutes`.
   */
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /**
   * Whether the rail is collapsed to icons.
   */
  protected readonly collapsed = signal(false);

  /**
   * Whether a backoffice session is open.
   */
  protected readonly adminMode = computed(() => this.adminSession.isAuthenticated());

  /**
   * Navigation chapters on offer, the backoffice ones replacing the public ones.
   */
  protected readonly navGroups = computed(() => (this.adminMode() ? ADMIN_NAV_GROUPS : NAV_GROUPS));

  /**
   * Languages the switcher offers, in display order.
   */
  protected readonly supportedLanguages = this.translation.supportedLanguages;

  /**
   * Active language.
   */
  protected readonly language = this.translation.language;

  /**
   * Utilities driven by the collapsed state.
   */
  protected readonly rail = computed(() => resolveRailClasses(this.collapsed()));

  /**
   * Utilities driven by the drawer's open state.
   */
  protected readonly drawer = computed(() => resolveDrawerClasses(this.navigationPanel.isOpen()));

  /**
   * Language trigger utilities: collapsed-rail size plus open state.
   */
  protected readonly languageButtonClass = computed(() => {
    const stateClass = this.languageMenuOpen() ? LANGUAGE_MENU_OPEN_CLASS : '';
    return `${this.rail().languageButton} ${stateClass}`;
  });

  /**
   * Whether the language panel is open.
   */
  protected readonly languageMenuOpen = signal(false);

  /**
   * Language panel id, for the trigger's `aria-controls`.
   */
  protected readonly languageMenuId = 'sidebar-language-menu';

  /**
   * Language switcher host, to close the panel on outside clicks.
   */
  private readonly languageMenuElement = viewChild<ElementRef<HTMLElement>>('languageMenu');

  /**
   * Drawer close button, focused when the drawer opens.
   */
  private readonly closeMenuButton = viewChild<ElementRef<HTMLButtonElement>>('closeMenuButton');

  /**
   * Synchronization status, behind the last synchronization label and the status dot.
   */
  private readonly statusResource = this.synchronizationApi.status;

  /**
   * "In progress", the time since the last synchronization, or a loading/error fallback.
   */
  protected readonly lastSyncLabel = computed(() => {
    const status = resourceValue(this.statusResource, null);

    if (!status) {
      return this.translation.translate(
        this.statusResource.error() ? 'sidebar.lastSync.error' : 'sidebar.lastSync.loading',
      );
    }

    if (status.inProgress) {
      return this.translation.translate('sidebar.lastSync.inProgress');
    }

    return status.lastCompletedAt
      ? formatElapsed(status.lastCompletedAt, this.now(), this.translation.language())
      : this.translation.translate('sidebar.lastSync.unknown');
  });

  /**
   * Clock of the elapsed time, ticking every half minute.
   */
  private readonly now = signal(Date.now());

  /**
   * Whether the last synchronization is late while none runs.
   */
  protected readonly syncStale = computed(() => {
    const status = resourceValue(this.statusResource, null);
    return (
      !!status?.lastCompletedAt &&
      !status.inProgress &&
      isSynchronizationStale(status.lastCompletedAt, this.now())
    );
  });

  /**
   * Exact time, lateness warning, then what a synchronization does.
   */
  protected readonly lastSyncTooltip = computed(() => {
    const t = (key: string, params?: Record<string, string>): string =>
      this.translation.translate(`sidebar.lastSync.${key}`, params);
    const lastCompletedAt = resourceValue(this.statusResource, null)?.lastCompletedAt;
    const parts = [
      lastCompletedAt
        ? t('at', {
            date: formatSynchronizationTimestamp(lastCompletedAt, this.translation.language()),
          })
        : null,
      this.syncStale() ? t('stale') : null,
      t('tooltip'),
    ];
    return parts.filter((part) => part !== null).join(' ');
  });

  /**
   * Backend availability, inferred from the status resource.
   */
  protected readonly apiStatus = computed<'online' | 'offline'>(() =>
    this.statusResource.error() ? 'offline' : 'online',
  );

  /**
   * Status dot's accessible name and tooltip.
   */
  protected readonly apiStatusLabel = computed(() =>
    this.translation.translate(`sidebar.lastSync.status.${this.apiStatus()}`),
  );

  constructor() {
    const clock = setInterval(() => this.now.set(Date.now()), SIDEBAR_CLOCK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(clock));

    // After render: the closed panel is `visibility: hidden`, so its button is focusable only then.
    afterRenderEffect(() => {
      if (this.navigationPanel.isOpen()) {
        this.closeMenuButton()?.nativeElement.focus();
      }
    });
  }

  /**
   * Whether `item` is active for the current route.
   */
  protected isNavItemActive(item: NavItem): boolean {
    return isNavItemActive(this.currentUrl(), item);
  }

  /**
   * Active-state utilities of `item`, empty when inactive.
   */
  protected navActiveClass(item: NavItem): string {
    return this.isNavItemActive(item) ? NAV_ACTIVE_CLASS : '';
  }

  /**
   * Toggles the collapsed rail.
   */
  protected toggleCollapsed(): void {
    this.collapsed.update((collapsed) => !collapsed);
  }

  /**
   * Expands the collapsed rail on a click outside any button or link.
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
   * Closes the drawer, returning focus to its opener; a no-op on the rail.
   */
  protected closeMobileMenu(): void {
    this.navigationPanel.close();
  }

  /**
   * Closes the drawer after navigating and focuses the content, since the burger is destroyed.
   */
  protected onNavItemActivated(): void {
    if (!this.navigationPanel.isOpen()) {
      return;
    }

    this.navigationPanel.close();
    document.getElementById('main-content')?.focus();
  }

  /**
   * Switches language without awaiting: the dictionary swaps in on its own.
   */
  protected switchLanguage(language: Language): void {
    void this.translation.setLanguage(language);
    this.languageMenuOpen.set(false);
  }

  /**
   * Toggles the language panel.
   */
  protected toggleLanguageMenu(): void {
    this.languageMenuOpen.update((open) => !open);
  }

  /**
   * Signs out and leaves the guarded backoffice pages for the overview.
   */
  protected signOutOfAdmin(): void {
    this.adminSession.signOut();
    this.closeMobileMenu();
    void this.router.navigate(['/overview']);
  }

  /**
   * Closes the language panel on a click outside it.
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
   * Escape closes the innermost layer: the language panel, then the drawer.
   */
  protected onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') {
      return;
    }

    if (this.languageMenuOpen()) {
      this.languageMenuOpen.set(false);
      // Back to the trigger, or focus would fall to the document.
      this.languageMenuElement()?.nativeElement.querySelector<HTMLElement>('button')?.focus();
      return;
    }

    this.closeMobileMenu();
  }
}
