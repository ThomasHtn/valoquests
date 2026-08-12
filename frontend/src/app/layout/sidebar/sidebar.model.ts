/**
 * Primary navigation entry.
 *
 * Carries no icon: every entry shows the same hexagon marker, as in the mockup.
 */
export interface NavItem {
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
