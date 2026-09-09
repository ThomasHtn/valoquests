/**
 * Tailwind utilities the sidebar swaps with its state.
 *
 * Resolved in code rather than through `[class.lg:w-20]` bindings because an Angular class binding
 * cannot express a Tailwind variant prefix, and pairing a static `lg:` utility with a bound one for
 * the same property would leave their precedence to stylesheet order instead of the state. Every
 * `lg:` utility here applies from `lg` up only: below it the panel is a fixed-width drawer, and
 * collapsing is a rail-only affordance that must not leak into the mobile presentation.
 */

/**
 * Utilities driven by the rail's collapsed state.
 */
export interface RailClasses {
  /** Rail width: icons only, or icons and labels. */
  readonly width: string;
  /** Cursor over empty rail space while collapsed, signalling that a click re-expands it. */
  readonly cursor: string;
  /** Wordmark block, replaced by the "V" mark on a collapsed rail. */
  readonly brandBlock: string;
  /** Last-synchronization block, hidden in favour of its icon-only counterpart. */
  readonly syncBlock: string;
  /** Version line, the least essential thing in the footer. */
  readonly version: string;
  /** Navigation entry alignment: centered icon once collapsed, leading otherwise. */
  readonly navItem: string;
  /** Navigation label, kept in the DOM so the tab bar shares the same markup. */
  readonly navLabel: string;
  /** Chapter caption, replaced by a hairline on a collapsed rail. */
  readonly navGroupLabel: string;
  /** Hairline between chapters, shown only on a collapsed rail. */
  readonly navGroupRule: string;
  /** Footer row: stacked and centered on a collapsed rail, a left/right row elsewhere. */
  readonly footerContent: string;
  /** Language trigger size: a centered icon-only square on a collapsed rail. */
  readonly languageButton: string;
  /** Language code beside the trigger's icon, no room for it on a collapsed rail. */
  readonly languageCode: string;
  /** Language panel: opens into the content on a collapsed rail, too narrow to center it. */
  readonly languagePanel: string;
}

/**
 * Utilities driven by the drawer's open state, below `lg`.
 */
export interface DrawerClasses {
  /**
   * Position and visibility of the drawer. `invisible` rather than `hidden` keeps the panel out
   * of the tab order without taking it out of the layout mid-slide. Each state carries its own
   * transition because `visibility` must flip at once on the way in (so the drawer can take focus
   * the moment it opens) and only after the slide on the way out.
   */
  readonly panel: string;
  /** Scrim behind the drawer, timed the same way. */
  readonly scrim: string;
}

/**
 * Resolves the rail utilities for the given collapsed state.
 */
export function resolveRailClasses(collapsed: boolean): RailClasses {
  return {
    width: collapsed ? 'lg:w-20' : 'lg:w-64',
    cursor: collapsed ? 'lg:cursor-ew-resize' : '',
    brandBlock: collapsed ? 'lg:hidden' : 'lg:flex',
    syncBlock: collapsed ? 'lg:hidden' : 'lg:block',
    version: collapsed ? 'lg:hidden' : 'lg:block',
    navItem: collapsed ? 'lg:justify-center' : 'lg:justify-start',
    navLabel: collapsed ? 'lg:hidden' : '',
    navGroupLabel: collapsed ? 'lg:hidden' : '',
    navGroupRule: collapsed ? 'lg:block' : '',
    footerContent: collapsed ? 'lg:flex-col lg:items-center' : 'lg:flex-row lg:justify-between',
    languageButton: collapsed ? 'lg:w-9 lg:justify-center' : '',
    languageCode: collapsed ? 'lg:hidden' : '',
    languagePanel: collapsed ? 'lg:right-auto lg:left-0' : '',
  };
}

/**
 * Resolves the drawer utilities for the given open state.
 */
export function resolveDrawerClasses(open: boolean): DrawerClasses {
  return {
    panel: open
      ? 'visible translate-x-0 [transition:translate_300ms_ease-out,visibility_0s]'
      : 'invisible -translate-x-full [transition:translate_300ms_ease-out,visibility_0s_300ms]',
    scrim: open ? 'visible opacity-100' : 'invisible opacity-0',
  };
}

/**
 * Utilities layered onto the active navigation entry.
 */
export const NAV_ACTIVE_CLASS =
  ' bg-linear-to-r from-brand-500/20 to-transparent text-brand-500 before:bg-brand-500';

/**
 * Utilities of the open language trigger: the hover tint with gold text on top, so the open state
 * reads as a stronger version of the hover state rather than a distinct one.
 */
export const LANGUAGE_MENU_OPEN_CLASS = 'bg-brand-500/8 text-brand-500';
