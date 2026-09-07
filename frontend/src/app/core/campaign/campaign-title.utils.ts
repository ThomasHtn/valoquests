import { WEEKLY_TITLES, WeeklyTitle } from './campaign.model';

/**
 * The single title decorating a name wherever it appears outside the leaderboard's own list of
 * everything a player earned: the highest-priority one held, since no name ever carries more than
 * one badge. Ranking API titles arrays are already sorted by that priority (backend
 * `WeeklyTitleResolver`), so the first entry is always this one.
 */
export function primaryTitle(titles: readonly WeeklyTitle[]): WeeklyTitle | null {
  return titles[0] ?? null;
}

/**
 * Same resolution against a title-to-holder map (`CampaignToday.titles`): the first title in
 * ladder order this player holds.
 */
export function primaryTitleOf(
  titles: Partial<Record<WeeklyTitle, number>>,
  playerId: number,
): WeeklyTitle | null {
  return WEEKLY_TITLES.find((title) => titles[title] === playerId) ?? null;
}
