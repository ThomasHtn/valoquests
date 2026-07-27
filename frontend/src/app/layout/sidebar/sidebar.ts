import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideCalendarDays,
  LucideChartColumn,
  LucideHouse,
  LucidePanelLeftClose,
  LucideRefreshCw,
  LucideSettings,
  LucideTarget,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

import { Language, Translation } from '../../core/i18n/translation';
import { TranslatePipe } from '../../core/i18n/translate-pipe';
import { FLAG_BY_LANGUAGE, PENDING_NAV_ITEMS } from './sidebar.constants';

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
   * Sections without an implemented page yet.
   */
  protected readonly pendingNavItems = PENDING_NAV_ITEMS;

  /**
   * Language the footer switcher would activate on the next click.
   */
  protected readonly nextLanguage = computed<Language>(() => {
    const languages = this.translation.supportedLanguages;
    const currentIndex = languages.indexOf(this.translation.language());
    return languages[(currentIndex + 1) % languages.length];
  });

  /**
   * Display name of {@link nextLanguage}, used in the switcher's tooltip.
   */
  protected readonly nextLanguageName = computed(() =>
    this.translation.translate(`sidebar.language.names.${this.nextLanguage()}`),
  );

  /**
   * Flag of the currently active language, shown by the collapsed switcher.
   */
  protected readonly currentFlag = computed(() => FLAG_BY_LANGUAGE[this.translation.language()]);

  /**
   * Toggles the sidebar between its expanded and icon-only collapsed state.
   */
  protected toggleCollapsed(): void {
    this.collapsed.update((collapsed) => !collapsed);
  }

  /**
   * Switches the active application language to {@link nextLanguage}.
   *
   * Used by the collapsed rail, which only has room for a single flag and
   * cycles through {@link Translation.supportedLanguages} on click.
   */
  protected cycleLanguage(): void {
    void this.translation.setLanguage(this.nextLanguage());
  }

  /**
   * Switches the active application language directly to `language`.
   *
   * Used by the expanded footer, which shows every supported language as its
   * own flag so it can be picked in a single click.
   *
   * @param language - The language selected from the footer language switcher.
   */
  protected setLanguage(language: Language): void {
    void this.translation.setLanguage(language);
  }

  /**
   * Resolves the flag emoji for `language`.
   *
   * @param language - The language whose flag should be displayed.
   * @returns The flag emoji for `language`.
   */
  protected flag(language: Language): string {
    return FLAG_BY_LANGUAGE[language];
  }
}
