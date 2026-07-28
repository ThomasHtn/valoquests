import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
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

import { TranslatePipe } from '../../core/i18n/translate-pipe';
import { NAV_ITEMS } from './sidebar.constants';

/**
 * Persistent navigation sidebar.
 *
 * Displays the primary navigation and the last synchronization time. Can be
 * collapsed to an icon-only rail.
 */
@Component({
  selector: 'app-sidebar',
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
  styleUrl: './sidebar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sidebar {
  /**
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

  /**
   * Primary navigation entries.
   */
  protected readonly navItems = NAV_ITEMS;

  /**
   * Timestamp of the last player synchronization, shown in the footer.
   *
   * Placeholder until synchronization status is wired to a data-access service.
   */
  protected readonly lastSyncLabel = '08/07/2025 - 10:30';

  /**
   * Toggles the sidebar between its expanded and icon-only collapsed state.
   */
  protected toggleCollapsed(): void {
    this.collapsed.update((collapsed) => !collapsed);
  }
}
