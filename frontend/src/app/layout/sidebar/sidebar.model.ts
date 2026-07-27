/**
 * Icon of a {@link PendingNavItem}, matched against a `@switch` in the
 * template since each Lucide icon is its own standalone directive.
 */
export type PendingNavIcon =
  'users' | 'chart-column' | 'target' | 'calendar-days' | 'trophy' | 'settings';

/**
 * Navigation entry for a section without an implemented page yet.
 */
export interface PendingNavItem {
  /**
   * Icon displayed for this section.
   */
  readonly icon: PendingNavIcon;

  /**
   * Suffix appended to `sidebar.nav.` to resolve this section's label.
   */
  readonly labelKey: string;
}
