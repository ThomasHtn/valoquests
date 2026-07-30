import { Component, computed, inject, signal } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideChartColumn,
  LucideHouse,
  LucideMenu,
  LucideRefreshCw,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PlayersApi } from '@core/players/players-api';
import { NAV_ITEMS } from './sidebar.constants';
import { formatSynchronizationTimestamp, resolveLatestSynchronization } from './sidebar.utils';

/**
 * Persistent navigation sidebar.
 *
 * Displays the primary navigation and the last synchronization time. Renders as a vertical rail on
 * `lg` and above, where it can be collapsed to icons only, and as a bottom tab bar below it.
 *
 * The host is `display: contents` so the inner `<aside>` is itself the flex item of the
 * application shell, which is what lets it reorder from first (rail) to last (tab bar).
 */
@Component({
  selector: 'app-sidebar',
  host: { class: 'contents' },
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideHouse,
    LucideUsers,
    LucideChartColumn,
    LucideTrophy,
    LucideMenu,
    LucideRefreshCw,
    TranslatePipe,
    MatTooltip,
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
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

  /**
   * Primary navigation entries.
   */
  protected readonly navItems = NAV_ITEMS;

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
}
