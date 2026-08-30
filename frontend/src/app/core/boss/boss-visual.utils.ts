import { BossCategory } from './boss.model';

/**
 * Text color utility applied per boss category, from weakest to strongest.
 */
const BOSS_CATEGORY_COLORS: Readonly<Record<BossCategory, string>> = {
  MINOR: 'text-accent-green',
  STANDARD: 'text-accent-blue',
  ELITE: 'text-accent-red',
};

/**
 * Resolves the text color utility for a boss's category.
 *
 * Only the rules page colors a category today: the overview card and the campaign timeline both
 * render it as plain muted text beside the boss name.
 *
 * @param category - The boss's weight class.
 * @returns The Tailwind text color utility to apply.
 */
export function resolveBossCategoryColorClass(category: BossCategory): string {
  return BOSS_CATEGORY_COLORS[category];
}

/**
 * Resolves a week's position in its run as the two-digit boss number the interface leads with
 * (`01`, `02`, … up to a run's ten weeks), never the catalogue name — see design-review.md §B2 ter:
 * numbering makes the ten-week schedule (and its two elites) readable ahead of time, which a set of
 * proper names cannot.
 *
 * @param runWeekIndex - Position of the week inside its run, from one.
 * @returns The zero-padded boss number.
 */
export function resolveBossNumberLabel(runWeekIndex: number): string {
  return String(runWeekIndex).padStart(2, '0');
}

/*
 * No boss HP bar color helper here on purpose: the health bar is always `bg-accent-red` and
 * always drains, never a green/gold/red traffic light — red is the threat, and what remains of
 * it is what remains to beat. A bar that turned green at full health said the opposite of what
 * red means everywhere else in the app. Written as a plain Tailwind class at each call site, not
 * a computed value, since it never varies.
 */
