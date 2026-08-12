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
 * Ring and glow color utility applied to the boss portrait, per category.
 */
const BOSS_CATEGORY_PORTRAIT_CLASSES: Readonly<Record<BossCategory, string>> = {
  MINOR: 'ring-accent-green shadow-[0_0_18px_var(--color-accent-green)]',
  STANDARD: 'ring-accent-blue shadow-[0_0_18px_var(--color-accent-blue)]',
  ELITE: 'ring-accent-red shadow-[0_0_18px_var(--color-accent-red)]',
};

/**
 * Resolves the ring and glow color utility for a boss's portrait, matching its category.
 *
 * @param category - The boss's weight class.
 * @returns The Tailwind ring/shadow color utilities to apply around the portrait.
 */
export function resolveBossCategoryPortraitClass(category: BossCategory): string {
  return BOSS_CATEGORY_PORTRAIT_CLASSES[category];
}

/**
 * Share of hit points remaining below which the health bar turns red.
 */
const HP_BAR_LOW_THRESHOLD = 20;

/**
 * Share of hit points remaining below which the health bar turns gold rather than green.
 */
const HP_BAR_MEDIUM_THRESHOLD = 50;

/**
 * Resolves the health bar's fill color from the boss's remaining hit points, so the bar itself
 * signals how close the group is to defeating it, on top of the percentage it already tracks.
 *
 * @param remainingPercentage - Share of hit points the boss has left, from 0 to 100.
 * @returns The Tailwind background color utility to apply to the fill.
 */
export function resolveBossHpBarColorClass(remainingPercentage: number): string {
  if (remainingPercentage <= HP_BAR_LOW_THRESHOLD) {
    return 'bg-accent-red';
  }

  if (remainingPercentage <= HP_BAR_MEDIUM_THRESHOLD) {
    return 'bg-accent-gold';
  }

  return 'bg-accent-green';
}
