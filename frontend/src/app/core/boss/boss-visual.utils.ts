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
 * The weight class each week of a run fights, first week to last.
 *
 * Mirrors `DefaultScoringRuleset#bossCategoryForRunWeek`. The class is *scheduled*, not drawn: weeks
 * five and ten are the run's two peaks, each followed by a minor to breathe. That is the single most
 * actionable fact the campaign holds — it is what lets a squad say "we save ourselves for the
 * fifth" — and it used to live only in the rules page's own constants, so the timeline that draws
 * those very ten weeks could not reach it.
 *
 * Lives here rather than in `pages/rules/`: two screens read it now, and it is a rule of the domain
 * rather than a fixture of one page.
 */
export const BOSS_CATEGORY_LADDER: readonly BossCategory[] = [
  'MINOR',
  'STANDARD',
  'STANDARD',
  'STANDARD',
  'ELITE',
  'MINOR',
  'STANDARD',
  'STANDARD',
  'STANDARD',
  'ELITE',
];

/**
 * The class a given week of a run fights, for a week whose boss has not been drawn yet.
 *
 * Returns `null` past the ladder's length rather than wrapping: a run is ten weeks, and a caller
 * asking for an eleventh is asking about a week that does not exist.
 *
 * @param runWeekIndex - Position of the week inside its run, from one.
 * @returns The scheduled weight class, or `null` outside the run.
 */
export function resolveScheduledBossCategory(runWeekIndex: number): BossCategory | null {
  return BOSS_CATEGORY_LADDER[runWeekIndex - 1] ?? null;
}

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
