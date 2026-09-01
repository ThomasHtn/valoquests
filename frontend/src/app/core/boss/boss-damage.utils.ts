/**
 * Damage one player dealt to a week's boss, from their ranking entry.
 *
 * Their weekly total minus the regularity bonus, which is the one component that stays out of the
 * fight — it rewards showing up rather than output. This is the same subtraction
 * `DefaultBossQueryService#totalDamageDealt` makes to fill the health bar, so a week's rows add up
 * to the bar they sit under.
 *
 * Shared rather than restated at each call site: two screens now break a health bar down per player
 * — the campaign's ranking and the accueil's confrontation band — and a second copy of this
 * subtraction is a second chance to forget it and print rows that overshoot their own bar.
 *
 * @param entry - The player's ranking entry, live or finalized.
 * @returns The damage that week's boss took from them.
 */
export function bossDamageOf(entry: {
  readonly totalDamage: number;
  readonly regularityBonus: number;
}): number {
  return entry.totalDamage - entry.regularityBonus;
}
