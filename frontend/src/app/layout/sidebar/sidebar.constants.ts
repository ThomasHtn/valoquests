import { Language } from '../../core/i18n/translation';
import { PendingNavItem } from './sidebar.model';

/**
 * Flag emoji representing each supported language in the footer switcher.
 *
 * Flags are locale-invariant symbols, not translated strings, so they are
 * kept here rather than in the i18n dictionaries.
 */
export const FLAG_BY_LANGUAGE: Readonly<Record<Language, string>> = {
  fr: '🇫🇷',
  en: '🇬🇧',
};

/**
 * Sections rendered without a `routerLink` since they have no page yet.
 *
 * Kept as data rather than repeated markup so the shared layout and long
 * Tailwind class list are written once and iterated with `@for`.
 */
export const PENDING_NAV_ITEMS: readonly PendingNavItem[] = [
  { icon: 'users', labelKey: 'players' },
  { icon: 'chart-column', labelKey: 'comparison' },
  { icon: 'target', labelKey: 'challenges' },
  { icon: 'calendar-days', labelKey: 'weeklyChallenges' },
  { icon: 'trophy', labelKey: 'ranking' },
  { icon: 'settings', labelKey: 'settings' },
];
