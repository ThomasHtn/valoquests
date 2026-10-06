import { RankingEntry } from '@core/ranking/ranking.model';

import { WEEKLY_TITLES } from '../campaign.constants';
import { WeeklyTitle } from './campaign-title.model';

/**
 * Single badge of a name: the first title, already priority-sorted by `WeeklyTitleResolver`.
 */
export function primaryTitle(titles: readonly WeeklyTitle[]): WeeklyTitle | null {
  return titles[0] ?? null;
}

/**
 * Single badge of a player from a title-to-holder map: the first title in ladder order they hold.
 */
export function primaryTitleOf(
  titles: Partial<Record<WeeklyTitle, number>>,
  playerId: number,
): WeeklyTitle | null {
  return WEEKLY_TITLES.find((title) => titles[title] === playerId) ?? null;
}

/**
 * Badge title of each ranked player id; players without a title are left out.
 */
export function buildTitlesByPlayer(
  ranking: readonly RankingEntry[],
): ReadonlyMap<number, WeeklyTitle> {
  const byPlayer = new Map<number, WeeklyTitle>();
  for (const entry of ranking) {
    const title = primaryTitle(entry.titles);
    if (title !== null) {
      byPlayer.set(entry.player.id, title);
    }
  }
  return byPlayer;
}
