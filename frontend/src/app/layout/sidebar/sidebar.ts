import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { Tooltip } from '@shared/tooltip/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideBookOpen,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideMenu,
  LucideRefreshCw,
  LucideSkull,
  LucideTarget,
  LucideTrophy,
  LucideUsers,
  LucideX,
} from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Language } from '@core/i18n/translation.model';
import { PlayersApi } from '@core/players/players-api';
import { NAV_ITEMS } from './sidebar.constants';
import { formatSynchronizationTimestamp, resolveLatestSynchronization } from './sidebar.utils';

/**
 * Persistent navigation sidebar.
 *
 * Displays the primary navigation, the last synchronization time and the language switch. Renders
 * as a vertical rail on `lg` and above, where it can be collapsed to icons only. Below that
 * breakpoint the same panel becomes a drawer sliding in from the left, opened from the bar's own
 * mobile header, so a phone keeps the full labelled navigation instead of a truncated tab bar.
 *
 * The host is `display: contents` so the mobile header and the `<aside>` are themselves flex items
 * of the application shell: the header stacks above the routed content, and the rail sits beside
 * it once the shell switches to a row on `lg`.
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
    RouterLinkActive,
    LucideBookOpen,
    LucideLanguages,
    LucideLayoutDashboard,
    LucideMenu,
    LucideRefreshCw,
    LucideSkull,
    LucideTarget,
    LucideTrophy,
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
   * Injector used to defer the drawer's focus moves to after the state change has been rendered.
   */
  private readonly injector = inject(Injector);

  /**
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

  /**
   * Whether the mobile drawer is open. Meaningless from `lg` up, where the panel is a static rail
   * that is always on screen.
   */
  protected readonly mobileMenuOpen = signal(false);

  /**
   * Id of the drawer panel, referenced by the mobile header's `aria-controls`.
   */
  protected readonly mobileMenuId = 'sidebar-panel';

  /**
   * Primary navigation entries.
   */
  protected readonly navItems = NAV_ITEMS;

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
   * {@link mobileMenuOpen}. The rail restores both from `lg` up through static `lg:` utilities.
   *
   * `invisible` rather than `hidden`: it keeps the panel out of the tab order and out of the
   * accessibility tree while closed, but still lets the slide transition run, since `visibility`
   * interpolates discretely and therefore holds `visible` for the whole exit.
   */
  protected readonly drawerClass = computed(() =>
    this.mobileMenuOpen() ? 'visible translate-x-0' : 'invisible -translate-x-full',
  );

  /**
   * Opacity and visibility utilities driving the scrim behind the drawer, for the same reason as
   * {@link drawerClass}.
   */
  protected readonly scrimClass = computed(() =>
    this.mobileMenuOpen() ? 'visible opacity-100' : 'invisible opacity-0',
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
   * Mobile header's burger button, refocused when the drawer closes so keyboard focus returns to
   * the control that opened it rather than to the top of the document.
   */
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

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
   * Opens the mobile drawer and moves focus into it.
   *
   * The focus move is deferred to the next render: while closed the panel is `visibility: hidden`,
   * which makes its controls unfocusable, and the class driving that only lands once the signal
   * change has been rendered.
   */
  protected openMobileMenu(): void {
    this.mobileMenuOpen.set(true);
    afterNextRender(() => this.closeMenuButton()?.nativeElement.focus(), {
      injector: this.injector,
    });
  }

  /**
   * Closes the mobile drawer and returns focus to the burger button that opened it.
   *
   * Guarded on the open state because the navigation entries call this on every activation, rail
   * included, where there is no drawer to close and no focus to move.
   */
  protected closeMobileMenu(): void {
    if (!this.mobileMenuOpen()) {
      return;
    }

    this.mobileMenuOpen.set(false);
    this.menuButton()?.nativeElement.focus();
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
