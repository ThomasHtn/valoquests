import { Component, computed, inject, signal } from '@angular/core';
import { Tooltip } from '@shared/tooltip/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideMenu, LucideRefreshCw } from '@lucide/angular';

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
 * as a vertical rail on `lg` and above, where it can be collapsed to icons only, and as a bottom
 * tab bar below it.
 *
 * The host is `display: contents` so the inner `<aside>` is itself the flex item of the
 * application shell, which is what lets it reorder from first (rail) to last (tab bar).
 */
@Component({
  selector: 'app-sidebar',
  host: { class: 'contents' },
  imports: [RouterLink, RouterLinkActive, LucideMenu, LucideRefreshCw, TranslatePipe, Tooltip],
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
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

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
   * breakpoint the navigation is a full-width tab bar.
   */
  protected readonly railWidthClass = computed(() => (this.collapsed() ? 'lg:w-20' : 'lg:w-64'));

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
   * Direction utility applied to the language switcher: the pair stacks on a collapsed rail, which
   * is too narrow to hold two buttons side by side, and stays a row everywhere else.
   *
   * Resolved here for the same reason as {@link navItemClass}: a Tailwind variant prefix cannot be
   * expressed through an Angular class binding.
   */
  protected readonly languageGroupClass = computed(() => (this.collapsed() ? 'lg:flex-col' : ''));

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
   * Switches the application to `language`.
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
  }
}
