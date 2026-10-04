import { DrawerClasses, RailClasses } from './sidebar.model';

/**
 * Rail utilities for the given collapsed state.
 */
export function resolveRailClasses(collapsed: boolean): RailClasses {
  return {
    width: collapsed ? 'lg:w-20' : 'lg:w-64',
    cursor: collapsed ? 'lg:cursor-ew-resize' : '',
    brandBlock: collapsed ? 'lg:hidden' : 'lg:flex',
    syncBlock: collapsed ? 'lg:hidden' : 'lg:block',
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
 * Drawer utilities for the given open state.
 */
export function resolveDrawerClasses(open: boolean): DrawerClasses {
  return {
    panel: open
      ? 'visible translate-x-0 [transition:translate_420ms_var(--ease-out-expo),visibility_0s]'
      : 'invisible -translate-x-full [transition:translate_240ms_var(--ease-in-quick),visibility_0s_240ms]',
    scrim: open ? 'visible opacity-100' : 'invisible opacity-0',
  };
}
