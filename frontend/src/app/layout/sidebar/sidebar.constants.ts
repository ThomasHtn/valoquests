import { NavItem } from './sidebar.model';

/**
 * Primary navigation entries, in display order.
 *
 * Kept as data rather than repeated markup so the shared layout and long
 * Tailwind class list are written once and iterated with `@for`. Entries
 * without a `routerLink` have no page yet and render as inert.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { icon: 'house', labelKey: 'overview', routerLink: '/' },
  { icon: 'users', labelKey: 'players' },
  { icon: 'chart-column', labelKey: 'comparison' },
  { icon: 'trophy', labelKey: 'ranking' },
];
