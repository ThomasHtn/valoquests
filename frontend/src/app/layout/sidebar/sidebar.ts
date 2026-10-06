import {
  afterRenderEffect,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';

import {
  LucideDynamicIcon,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideLogOut,
  LucideMenu,
  LucideX,
} from '@lucide/angular';
import { filter, map } from 'rxjs';

import { AdminSession } from '@core/admin/session/admin-session';
import { formatCampaignDateTime, formatElapsed } from '@core/date/date-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { SynchronizationApi } from '@core/synchronization/synchronization-api';
import { NavigationPanel } from '@layout/navigation-panel/navigation-panel';
import { FocusTrap } from '@shared/focus-trap/focus-trap';
import { Tooltip } from '@shared/tooltip/tooltip';

import { ADMIN_NAV_GROUPS, NAV_GROUPS, SIDEBAR_CLOCK_MS } from './sidebar.constants';
import { NavItem, SyncHealth } from './sidebar.model';
import { isNavItemActive, isSynchronizationStale } from './sidebar.utils';

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
    LucideDynamicIcon,
    LucideLanguages,
    LucideLayoutDashboard,
    LucideLogOut,
    LucideMenu,
    LucideX,
    TranslatePipe,
    Tooltip,
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
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
   * Clock of the elapsed time, ticking every half minute.
   */
  private readonly now = signal(Date.now());

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
    const translateSync: TranslateFn = (key, params) =>
      this.translation.translate(`sidebar.lastSync.${key}`, params);
    const lastCompletedAt = resourceValue(this.statusResource, null)?.lastCompletedAt;
    const parts = [
      lastCompletedAt
        ? translateSync('at', {
            date: formatCampaignDateTime(lastCompletedAt, this.translation.language()),
          })
        : null,
      this.syncStale() ? translateSync('stale') : null,
      translateSync('tooltip'),
    ];
    return parts.filter((part) => part !== null).join(' ');
  });

  /**
   * Backend availability, inferred from the status resource.
   */
  private readonly apiStatus = computed<'online' | 'offline'>(() =>
    this.statusResource.error() ? 'offline' : 'online',
  );

  /**
   * Status dot's accessible name and tooltip.
   */
  protected readonly apiStatusLabel = computed(() =>
    this.translation.translate(`sidebar.lastSync.status.${this.apiStatus()}`),
  );

  /**
   * Status dot tone: backend down, synchronization late, or all fresh.
   */
  protected readonly syncHealth = computed<SyncHealth>(() => {
    if (this.apiStatus() === 'offline') {
      return 'offline';
    }
    return this.syncStale() ? 'stale' : 'fresh';
  });

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
   * Switches language without awaiting: it changes once its dictionary has loaded.
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
