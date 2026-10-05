import type { LucideIcon } from '@lucide/angular';

/**
 * Navigation chapter: a caption over its entries.
 */
export interface NavGroup {
  /**
   * Suffix of the `sidebar.group.` caption key.
   */
  readonly labelKey: string;

  /**
   * Entries, in display order.
   */
  readonly items: readonly NavItem[];
}

/**
 * Primary navigation entry.
 */
export interface NavItem {
  /**
   * Suffix of the `sidebar.nav.` label key.
   */
  readonly labelKey: string;

  /**
   * Pictogram, the only identifier on the collapsed rail.
   */
  readonly icon: LucideIcon;

  /**
   * Target route; omitted entries render inert.
   */
  readonly routerLink?: string;

  /**
   * Extra URL prefixes that also activate the entry, for a section's second page.
   */
  readonly activeRoutes?: readonly string[];

  /**
   * Whether only an exact URL match activates the entry, for a route others start with.
   */
  readonly exactMatch?: boolean;
}

/**
 * Health of the synchronization readout, the status dot's tone.
 */
export type SyncHealth = 'fresh' | 'stale' | 'offline';
