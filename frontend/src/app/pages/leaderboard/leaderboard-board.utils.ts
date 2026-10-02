import { Campaign, CampaignHistory, WeeklyTitle } from '@core/campaign/campaign.model';
import { WEEK_DAYS } from '@core/date/date-time.constants';
import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { RankingEntry } from '@core/ranking/ranking.model';
import { WeekOption } from './leaderboard.model';

/**
 * Pure helpers of the leaderboard: figures, dates and campaign placement, with no i18n service.
 */

/**
 * The figure each title is awarded on, the way the backend awards them.
 */
export function resolveTitleMeasures(entry: RankingEntry): Partial<Record<WeeklyTitle, number>> {
  return {
    MECHANIC: entry.components,
    QUARTERMASTER: entry.food,
    REGULAR: entry.streakDays,
    SCOUT: entry.completedChallenges + entry.completedDailyChallenges,
  };
}

/**
 * Challenges a week draws: its weekly ones plus a daily every morning.
 */
export function weekChallengeCeiling(weeklyCount: number): number {
  return weeklyCount + WEEK_DAYS;
}

/**
 * Where a Monday falls: the running campaign's own week list first, then every closed campaign
 * by its first and last Mondays. Outside all of them, no index and no group.
 */
export function placeWeekInCampaign(
  weekStart: string,
  campaign: Campaign | null,
  history: readonly CampaignHistory[],
): Pick<WeekOption, 'index' | 'group'> {
  const week = campaign?.weeks.find((candidate) => candidate.weekStart === weekStart);
  if (week && campaign) {
    return { index: week.weekIndex, group: campaign.id };
  }
  const closed = history.find(
    (candidate) => weekStart >= candidate.firstWeekStart && weekStart <= candidate.lastWeekStart,
  );
  if (closed) {
    return {
      index: daysBetween(closed.firstWeekStart, weekStart) / WEEK_DAYS + 1,
      group: closed.id,
    };
  }
  return { index: null, group: null };
}

/**
 * Keeps the week the reader picked across a background reload; a new live week resets to it.
 *
 * @param weekStarts - Mondays the page can show, the live week first.
 * @param previous - The previous list and pick, absent on the first resolution.
 * @returns The Monday on screen.
 */
export function resolveSelectedWeek(
  weekStarts: readonly string[],
  previous?: { readonly source: readonly string[]; readonly value: string | null },
): string | null {
  const live = weekStarts[0] ?? null;
  if (!previous || previous.source[0] !== live) {
    return live;
  }
  return previous.value !== null && weekStarts.includes(previous.value) ? previous.value : live;
}

/**
 * Monday to Sunday, the month spelled once when both days share it (`31 août – 6 sept.`).
 */
export function formatWeekSpan(weekStart: string, locale: string): string {
  const monday = localMidnight(weekStart);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + WEEK_DAYS - 1);
  const short = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
  if (monday.getMonth() === sunday.getMonth()) {
    return `${monday.getDate()} – ${short.format(sunday)}`;
  }
  return `${short.format(monday)} – ${short.format(sunday)}`;
}
