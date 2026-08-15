/**
 * Pictogram identifying a navigation entry.
 *
 * Modelled as a closed union rather than a free-form Lucide name so the template's `@switch` is
 * exhaustive: an entry can only ask for an icon the sidebar actually imports.
 */
export type NavIcon =
  | 'layout-dashboard'
  | 'target'
  | 'skull'
  | 'trophy'
  | 'users'
  | 'book-open'
  | 'refresh-cw'
  | 'user-cog'
  | 'database-backup';

/**
 * Primary navigation entry.
 */
export interface NavItem {
  /**
   * Suffix appended to `sidebar.nav.` to resolve this section's label.
   */
  readonly labelKey: string;

  /**
   * Pictogram shown ahead of the label, and the only thing identifying the entry on the collapsed
   * icon-only rail.
   */
  readonly icon: NavIcon;

  /**
   * Route to navigate to. Omitted for sections without an implemented page
   * yet, which render as inert entries.
   */
  readonly routerLink?: string;

  /**
   * Extra URL prefixes that also count as this entry being active, beyond {@link routerLink}
   * itself. Reserved for a section that owns a second page reached from within the first rather
   * than from the sidebar, such as the campaign's battle history — the sidebar still has only one
   * entry for the whole section, but that entry stays highlighted on both.
   */
  readonly activeRoutes?: readonly string[];

  /**
   * Whether the entry is highlighted only on an exact URL match.
   *
   * Reserved for the root route, which every other URL starts with. Every
   * other section owns its whole subtree, so its entry must stay highlighted
   * on child routes such as a player's profile under `/players`.
   */
  readonly exactMatch?: boolean;
}
