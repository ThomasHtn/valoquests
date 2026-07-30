/**
 * Icon of a {@link NavItem}, matched against a `@switch` in the template
 * since each Lucide icon is its own standalone directive.
 */
export type NavIcon = 'house' | 'users' | 'chart-column' | 'trophy';

/**
 * Primary navigation entry.
 */
export interface NavItem {
  /**
   * Icon displayed for this section.
   */
  readonly icon: NavIcon;

  /**
   * Suffix appended to `sidebar.nav.` to resolve this section's label.
   */
  readonly labelKey: string;

  /**
   * Route to navigate to. Omitted for sections without an implemented page
   * yet, which render as inert entries.
   */
  readonly routerLink?: string;

  /**
   * Whether the entry is highlighted only on an exact URL match.
   *
   * Reserved for the root route, which every other URL starts with. Every
   * other section owns its whole subtree, so its entry must stay highlighted
   * on child routes such as a player's profile under `/players`.
   */
  readonly exactMatch?: boolean;
}
