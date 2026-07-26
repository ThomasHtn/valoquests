import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideCalendarDays,
  LucideChartColumn,
  LucideHouse,
  LucideLanguages,
  LucidePanelLeftClose,
  LucideRefreshCw,
  LucideSettings,
  LucideTarget,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

import { Language, Translation } from '../../core/i18n/translation';
import { TranslatePipe } from '../../core/i18n/translate-pipe';

/**
 * Persistent navigation sidebar.
 *
 * Displays the primary navigation, the language switcher and the last
 * synchronization time. Can be collapsed to an icon-only rail.
 */
@Component({
  selector: 'app-sidebar',
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideHouse,
    LucideUsers,
    LucideChartColumn,
    LucideTarget,
    LucideCalendarDays,
    LucideTrophy,
    LucideSettings,
    LucidePanelLeftClose,
    LucideRefreshCw,
    LucideLanguages,
    TranslatePipe,
    MatTooltip,
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sidebar {
  /**
   * i18n service exposing the active language and the language switcher options.
   */
  protected readonly translation = inject(Translation);
  /**
   * Whether the sidebar is rendered as an icon-only collapsed rail.
   */
  protected readonly collapsed = signal(false);

  /**
   * Toggles the sidebar between its expanded and icon-only collapsed state.
   */
  protected toggleCollapsed(): void {
    this.collapsed.update((collapsed) => !collapsed);
  }

  /**
   * Switches the active application language.
   *
   * @param language - The language selected from the footer language switcher.
   */
  protected setLanguage(language: Language): void {
    void this.translation.setLanguage(language);
  }
}
