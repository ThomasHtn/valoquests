/**
 * Navigation pictogram, a closed union so the template's `@switch` covers every imported icon.
 */
export type NavIcon =
  | 'layout-dashboard'
  | 'target'
  | 'trophy'
  | 'users'
  | 'book-open'
  | 'refresh-cw'
  | 'user-cog'
  | 'database-backup'
  | 'palette'
  | 'flag';

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
  readonly icon: NavIcon;

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
 * Resolved in code: an Angular class binding cannot express a Tailwind `lg:` variant.
 */

/**
 * Utilities driven by the rail's collapsed state (`lg` and up only).
 */
export interface RailClasses {
  /**
   * Rail width.
   */
  readonly width: string;
  /**
   * Cursor over empty collapsed rail space, which expands it on click.
   */
  readonly cursor: string;
  /**
   * Wordmark block, replaced by the "V" mark when collapsed.
   */
  readonly brandBlock: string;
  /**
   * Last-synchronization block, hidden when collapsed.
   */
  readonly syncBlock: string;
  /**
   * Navigation entry alignment.
   */
  readonly navItem: string;
  /**
   * Navigation label, hidden rather than removed so the drawer shares the markup.
   */
  readonly navLabel: string;
  /**
   * Chapter caption, replaced by a hairline when collapsed.
   */
  readonly navGroupLabel: string;
  /**
   * Hairline between chapters, shown only when collapsed.
   */
  readonly navGroupRule: string;
  /**
   * Footer layout: stacked when collapsed, a row otherwise.
   */
  readonly footerContent: string;
  /**
   * Language trigger size.
   */
  readonly languageButton: string;
  /**
   * Language code beside the trigger icon, hidden when collapsed.
   */
  readonly languageCode: string;
  /**
   * Language panel position: opens into the content when collapsed.
   */
  readonly languagePanel: string;
}

/**
 * Utilities driven by the drawer's open state, below `lg`.
 */
export interface DrawerClasses {
  /**
   * Drawer position and visibility; `visibility` flips at once on open (to take focus) and only
   * after the slide on close.
   */
  readonly panel: string;
  /**
   * Scrim behind the drawer, timed the same way.
   */
  readonly scrim: string;
}
