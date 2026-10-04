import { WEEKLY_TITLES } from '../campaign.constants';
import { WeeklyTitle } from './campaign-title.model';

/**
 * Single badge of a name: the first title, already priority-sorted by `WeeklyTitleResolver`.
 */
export function primaryTitle(titles: readonly WeeklyTitle[]): WeeklyTitle | null {
  return titles[0] ?? null;
}

/**
 * Same from a title-to-holder map: the first title in ladder order this player holds.
 */
export function primaryTitleOf(
  titles: Partial<Record<WeeklyTitle, number>>,
  playerId: number,
): WeeklyTitle | null {
  return WEEKLY_TITLES.find((title) => titles[title] === playerId) ?? null;
}
