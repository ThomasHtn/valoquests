import { Season } from '@core/matches/season.model';

/**
 * Pure helpers of the player-profile page.
 */

/**
 * Resolves the id of the current season - the one flagged `active` - or the first (most-recent)
 * known season if none is active, or `null` if none are known yet.
 */
export function resolveCurrentSeasonId(seasons: readonly Season[]): number | null {
  return (seasons.find((season) => season.active) ?? seasons[0])?.id ?? null;
}

/**
 * Colour of the share the next match keeps: amber at full value, muted once the ladder has
 * started taking a cut. A red would overstate a rule that still pays half.
 */
export function resolveYieldToneClass(percent: number): string {
  return percent >= 100 ? 'text-brand-500' : 'text-text-secondary';
}
